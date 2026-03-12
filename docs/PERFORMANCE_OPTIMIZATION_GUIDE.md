# 성능 측정/최적화 가이드 (Alloy + Prometheus + Loki)

> 목적: "먼저 측정" 원칙으로 기준선을 만들고, 이후 개선 효과를 수치로 증명한다.

---

## 1) 관측 스택 방향: Promtail 대신 Alloy

결론: **Alloy 사용이 맞다.**

이유:
- PLG(= Prometheus/Loki/Grafana)에서 메트릭/로그/트레이스 수집 구성을 한 에이전트로 통합 가능
- Promtail은 사실상 로그 수집 전용이고, Alloy는 확장성과 파이프라인 유연성이 더 좋음
- 장기적으로 OTel 연동까지 가져가려면 Alloy가 운영/확장 측면에서 유리

권장 원칙:
- 로그는 구조화(JSON) 유지
- 고카디널리티 라벨(userId, requestId 원문 등) 최소화
- 메트릭은 endpoint/status/method 중심으로 집계

---

## 2) 이번 프로젝트에서 우선으로 볼 지표 (Top Priority)

### A. 사용자 체감 성능

1. `http_server_requests_seconds` (latency)
- 우선: `p95`, 그 다음 `p99`, `p50`
- 대상 endpoint: `/posts`, `/posts/{id}`, `/posts/top10`, `/posts/{id}/likes`

2. `http_server_requests_seconds_count` + `rate(...)`
- 처리량(RPS/TPS) 추적

3. `5xx`, `4xx` 비율
- 최적화 후 에러율 증가 여부 확인

### B. DB 병목

1. `hikaricp_connections_pending`
- **가장 중요**: DB 커넥션 부족 징후

2. `hikaricp_connections_active`, `hikaricp_connections_idle`
- 활성/유휴 커넥션 밸런스

3. `hikaricp_connections_timeout_total`
- 커넥션 획득 타임아웃 발생 여부

4. API별 SQL 개수
- N+1 개선 검증 핵심

### C. 리소스/런타임 안정성

1. `jvm_gc_pause_seconds`
- 긴 GC pause 여부 확인

2. `process_cpu_usage`, `jvm_memory_used_bytes`
- CPU/메모리 포화 감시

---

## 3) 핵심 KPI (포트폴리오 표준)

개선 전/후 비교는 아래 5개만 먼저 고정:

1. `GET /posts` p95 latency
2. `POST /posts` p95 latency
3. 전체 5xx rate
4. `hikaricp_connections_pending` 최대값
5. API 1회 호출당 SQL 개수

이 5개가 먼저 내려가야 "성능 개선"으로 설득력 있음.

---

## 4) 측정 절차 (반드시 같은 조건으로 반복)

1. 테스트 환경 고정
- 동일 인스턴스 스펙
- 동일 DB 데이터셋
- 동일 JVM 옵션

2. 부하 시나리오 고정
- 예: `vus=50`, `duration=10m`, warm-up 2m

3. 3회 반복 후 중앙값 채택
- 1회 결과로 결론 금지

4. 결과 저장
- Grafana 대시보드 캡처
- k6 요약 결과
- SQL 카운트 비교표

---

## 5) API별 측정 우선순위

1. `GET /posts`
- 목록 조회 성능과 N+1 영향 확인

2. `POST /posts`
- 이미지 업로드 + 트랜잭션 병목 확인

3. `POST /posts/{postId}/likes`
- 동시성 충돌/카운트 정합성 확인

4. `GET /posts/top10`
- 캐시 도입 전/후 효과 확인

---

## 6) 개선 항목별 검증 포인트

### 6-1. Like 중복 방지(unique)
- 지표: 에러율(충돌 처리), like 카운트 정합성
- 성공 기준: 동시 토글 시 중복 row 0건

### 6-2. 댓글 카운트 정합성
- 지표: `post_status.comment_count` vs 실댓글 count diff
- 성공 기준: diff=0

### 6-3. S3 업로드 트랜잭션 분리
- 지표: `POST /posts` p95, `hikaricp_connections_pending`
- 성공 기준: p95 감소 + pending 피크 감소

### 6-4. readOnly 트랜잭션 분리
- 지표: 조회 API p95, CPU 사용량
- 성공 기준: p95 소폭 개선 + CPU 안정

### 6-5. Redis 캐시 도입
- 지표: endpoint p95, DB SQL 수
- 성공 기준: 캐시 대상 API SQL 호출 급감

---

## 7) Alloy 운영 팁 (실무 관점)

- 초기에는 수집 범위를 좁게 시작
- 라벨 폭발 방지: path 템플릿화(`/posts/{id}`) 적용
- 로그 샘플링은 에러 로그 우선 보존
- 대시보드는 "서비스/DB/JVM" 3패널로 분리

---

## 8) 최종 보고서 템플릿

| 항목 | 개선 전 | 개선 후 | 변화율 | 근거 |
|---|---:|---:|---:|---|
| GET /posts p95(ms) |  |  |  | Grafana panel 링크 |
| POST /posts p95(ms) |  |  |  | Grafana panel 링크 |
| 5xx rate(%) |  |  |  | Grafana panel 링크 |
| Hikari pending max |  |  |  | Grafana panel 링크 |
| API당 SQL 수 |  |  |  | 로그/카운트 스크린샷 |

---

## 9) 현재 시점 결론

- 지금 단계에서 가장 맞는 순서:
1. Alloy 기반 지표 수집 안정화
2. 기준선 측정 완료
3. 저비용 개선부터 적용
4. 구조 개선 적용
5. 동일 시나리오 재측정 및 수치 비교
