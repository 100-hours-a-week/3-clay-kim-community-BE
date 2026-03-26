# 성능 기준선 측정 결과 (Before)

## 측정 환경

| 항목 | 스펙 |
|---|---|
| App EC2 | t3.small (2 vCPU, 2GB RAM) |
| MySQL EC2 | t3.small (2 vCPU, 2GB RAM) |
| Redis EC2 | t3.micro (2 vCPU, 1GB RAM) |
| Monitoring EC2 | t3.small (2 vCPU, 2GB RAM) |
| Spring Boot | 3.5.6 / Java 21 |
| MySQL | 8.x |
| HikariCP | 기본 설정 (max pool size: 10) |
| 데이터 규모 | 게시글 100만건, 좋아요 500만건 |

## 부하 테스트 조건

| 항목 | 설정 |
|---|---|
| 도구 | k6 |
| 동시 사용자 (VUs) | 50 |
| 테스트 시간 | 5분 |
| 대상 API | GET /posts, GET /posts/{postId}, GET /posts/top10 |
| 측정 일시 | 2026-03-16 10:14 |

## 측정 결과

### API별 응답 시간

| API | avg | med | p90 | p95 | p99 | max |
|---|---|---|---|---|---|---|
| GET /posts/top10 | 31s | 30s | 34s | 35s | 36s | 36s |
| GET /posts/{postId} | 16s | 29s | 30s | 30s | 30s | 30s |
| GET /posts | 12s | 2s | 30s | 30s | 30s | 30s |
| 전체 (http_req_duration) | 20s | 30s | 32s | 34s | 35s | 36s |

### 처리량 및 에러율

| 항목 | 수치 |
|---|---|
| RPS (초당 요청 수) | 2.36/s |
| 총 요청 수 | 780건 / 5분 |
| 에러율 | 45.4% |
| Request Waiting p95 | 34s |

### HikariCP 커넥션 풀 (Grafana)

| 항목 | 수치 |
|---|---|
| Pool Utilization | 100% (고갈) |
| Pending Threads (최대) | 40+ |
| Acquire Time (평균) | 25s |
| Usage Time (평균) | 15s |
| Max Pool Size | 10 (기본값) |

### 인프라 리소스 (Grafana)

| 항목 | 수치 |
|---|---|
| App EC2 CPU | 100% |
| MySQL EC2 CPU | ~80% |
| App EC2 Memory | ~65% |

## 근본 원인 분석

### 1. Top10 쿼리 풀 테이블 스캔 (핵심 원인)

Top10 인기글 조회 쿼리가 `posts` 테이블 100만 row를 **풀 스캔**하고 있었다.

```sql
-- EXPLAIN ANALYZE 결과: 2,571ms 소요
-> Sort: ps.like_count DESC  (actual time=2571..2571 rows=10 loops=1)
    -> Stream results  (cost=458881 rows=996906) (actual time=0.628..2446 rows=499476 loops=1)
        -> Nested loop inner join  (cost=458881 rows=996906) (actual time=0.619..2164 rows=499476 loops=1)
            -> Table scan on p  (cost=100695 rows=996906) (actual time=0.0321..618 rows=1000000 loops=1)
```

- `WHERE deleted = false AND type = 'COMPLETED'` 조건에 대한 **복합 인덱스가 없음**
- 단건 쿼리에 2.5초 소요 → 동시 요청 시 커넥션 풀 고갈의 직접적 원인

### 2. HikariCP 커넥션 풀 고갈 (연쇄 장애)

```
[원인] Top10 쿼리 2.5초/건
  → [1단계] 10개 커넥션 모두 2.5초짜리 쿼리에 점유
  → [2단계] 나머지 40개 요청이 커넥션 대기 (Pending 40+)
  → [3단계] connectionTimeout 30초 초과 → CannotCreateTransactionException
  → [결과] 전체 API 응답 시간 30초 (타임아웃), 에러율 45%
```

Top10 쿼리 하나가 서버 전체를 마비시키는 **연쇄 장애(Cascading Failure)** 패턴이다.

- `GET /posts` (목록 조회)도 단독 실행 시 수십ms 이내지만, 커넥션 풀 고갈로 30초 타임아웃
- `GET /posts/{postId}` (상세 조회)도 동일하게 영향

### 3. 추가 발견 사항

| 문제 | 영향도 | 설명 |
|---|---|---|
| `Post.updatePost()` NPE 버그 | HIGH | null 체크 순서가 반대 (`!title.isBlank()` 호출 후 `!= null` 체크) |
| `LikeService.getLikeCount()` 죽은 쿼리 | MEDIUM | 500만 row COUNT 실행 후 결과 미사용, post_statuses 값만 반환 |
| 읽기 메서드 readOnly 미적용 | LOW | 10개 읽기 메서드가 읽기-쓰기 트랜잭션으로 실행 |
| 댓글 삭제 시 카운트 미감소 | MEDIUM | 댓글 soft delete 시 post_statuses.comment_count 미감소 |
| 좋아요 중복 방지 제약 없음 | HIGH | 동시 요청 시 같은 user+post에 중복 좋아요 가능 |

## 개선 계획

위 문제들에 대해 다음 순서로 개선을 진행한다.

| Phase | 내용 | 예상 효과 |
|---|---|---|
| 1 | 복합 인덱스 추가 (`deleted + created_at`, `deleted + type`) | Top10 쿼리 2.5s → 수십ms, 커넥션 풀 고갈 해소 |
| 2 | 버그 수정 (NPE, 죽은 쿼리, 댓글 카운트, 유니크 제약) | 데이터 정합성 확보 |
| 3 | 읽기 전용 트랜잭션 readOnly 적용 | DB 리소스 절약 |
| 4 | Redis 캐싱 (Top10 cache-aside, TTL 5분) | DB 부하 추가 감소 |
| 5 | 조회수 Redis Write-Behind | 조회수 UPDATE 쿼리 수 감소 |

개선 후 동일 조건(50 VUs, 5분)으로 재측정하여 before/after 비교표를 작성할 예정이다.
