# 종주메이트 백엔드 개선 계획서 (현행 코드 기준)

> Spring Boot 3.5.6 / Java 21
> 목적: 포트폴리오에 바로 사용 가능한 형태로, "현재 상태"와 "개선 계획"을 분리해서 관리

---

## 1) 문서 사용 원칙

- 이 문서는 "계획" 문서다.
- 이미 구현된 항목은 `DONE`, 아직 미구현은 `TODO`, 부분 구현은 `PARTIAL`로 표기한다.
- 성능 수치는 추정치가 아니라 `실측값`만 기록한다.

---

## 2) 현재 상태 스냅샷 (코드 검증 기준)

| 항목 | 상태 | 근거 요약 |
|---|---|---|
| 요청 검증(Bean Validation) | TODO | Request DTO에 validation annotation 없음, controller `@Valid` 미사용 |
| Global Exception Handler 표준화 | PARTIAL | `CustomException`만 처리, validation/type mismatch/unhandled 예외 응답 표준화 미흡 |
| 좋아요 동시성 제어 | TODO | `PostLike(user_id, post_id)` 유니크 제약 없음 |
| 조회수 집계 전략 | TODO | 조회수 증가 호출이 주석 상태, Redis 기반 집계 미적용 |
| 댓글 카운트 정합성 | TODO | 댓글 등록 시 증가만 있고 삭제 시 감소 없음 |
| 로깅(AOP + MDC) | DONE | API AOP 로그 + MDC 필터 존재 |
| API Rate Limiting | TODO | 로그인/인증 API 요청 제한 없음 |
| 캐싱(Top10/타입 통계 등) | TODO | Redis 연결은 있으나 Spring Cache 전략 미적용 |
| 읽기 트랜잭션 분리(readOnly) | PARTIAL | 일부 조회만 readOnly, 전체 서비스에 일관 적용 안 됨 |
| S3 업로드/DB 트랜잭션 분리 | TODO | 이미지 업로드가 게시글 트랜잭션 내에서 수행 |
| 운영 SQL 로깅 최적화 | TODO | prod `show-sql: true` |
| Swagger/SpringDoc | DONE | 의존성 및 endpoint 사용 중 |
| 테스트 커버리지 | PARTIAL | 핵심 서비스 테스트 존재하나 통합/리포지토리/경계케이스 보강 필요 |

---

## 3) 우선순위 로드맵

### Phase A: 측정/기준선 확보 (먼저 수행)

1. 핵심 API 4개 선정
- `GET /posts`
- `GET /posts/{postId}`
- `POST /posts`
- `POST /posts/{postId}/likes`

2. 부하 시나리오 고정
- 동일한 데이터셋/동일 VU/동일 duration으로 3회 반복

3. 기준선 기록
- p50/p95/p99 latency
- RPS/TPS
- 에러율
- DB connection pending
- API별 SQL 수

### Phase B: 빠른 수정 (저비용 고효율)

1. `LikeService.getLikeCount()` 불필요 쿼리 제거
2. 댓글 삭제 시 comment count 감소 로직 추가
3. 조회 메서드 `@Transactional(readOnly=true)` 일괄 적용
4. prod SQL 로깅 비활성화

### Phase C: 구조 개선 (효과 큼)

1. `PostLike` 유니크 제약 추가 + 충돌 예외 처리
2. S3 업로드를 트랜잭션 밖으로 분리
3. Redis 캐시(Top10/타입 통계) 도입
4. 조회수 Redis write-behind 집계 도입

---

## 4) 포트폴리오 작성 규칙

### 작성 포맷 (항목당)

- 문제: 어떤 병목/리스크였는지
- 가설: 왜 느리거나 깨지는지
- 개선: 어떤 방식으로 바꿨는지
- 검증: 개선 전/후 수치
- 트레이드오프: 부작용과 보완책

### 금지 사항

- "10배 빨라졌다" 같은 추정 문구 단독 사용
- 근거 없는 기대값 표기
- 코드 기준과 문서 상태 불일치

---

## 5) 트래킹 템플릿

| ID | 개선 항목 | 상태 | 기준선 완료 | 구현 완료 | 회귀 테스트 | 실측 결과 링크 |
|---|---|---|---|---|---|---|
| BI-01 | Like 중복 방지(unique) | TODO |  |  |  |  |
| BI-02 | 댓글 카운트 정합성 | TODO |  |  |  |  |
| BI-03 | S3 업로드 트랜잭션 분리 | TODO |  |  |  |  |
| BI-04 | 읽기 트랜잭션 분리 | TODO |  |  |  |  |
| BI-05 | Redis 캐시(Top10/타입) | TODO |  |  |  |  |
| BI-06 | 조회수 Redis 집계 | TODO |  |  |  |  |
| BI-07 | prod SQL 로그 최적화 | TODO |  |  |  |  |

---

## 6) 다음 액션

- 먼저 성능 측정 문서(`PERFORMANCE_OPTIMIZATION_GUIDE.md`) 기준으로 기준선 수집
- 수집 완료 후 이 문서의 `트래킹 템플릿`을 채워서 포트폴리오 본문으로 전환
