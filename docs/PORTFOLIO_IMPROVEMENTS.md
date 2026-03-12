# 종주메이트 백엔드 개선사항 정리 (포트폴리오용)

> Spring Boot 3.5.6 / Java 21 / MySQL / Redis / AWS (S3, ECR, EC2)

---

## 1. 인증 전략 패턴 (Strategy Pattern) 적용

**문제**: 초기에는 Session 기반 인증을 사용했으나, 수평 확장(Scale-out) 시 세션 공유 문제가 예상됨

**개선**:
- `AuthenticationStrategy` 인터페이스를 도입하여 JWT / Session 인증을 전략 패턴으로 분리
- `auth.type` 프로퍼티 한 줄로 인증 방식 전환 가능
- JWT: Access Token(30분) + Refresh Token(7일)을 HTTP-only 쿠키에 저장

**포인트**:
- OCP(개방-폐쇄 원칙) 준수 — 새로운 인증 방식 추가 시 기존 코드 수정 불필요
- HTTP-only 쿠키로 XSS 공격 방어
- 서버 무상태(Stateless) 전환으로 수평 확장 대응

---

## 2. 커서 기반 페이지네이션 도입

**문제**: Offset 기반 페이지네이션은 데이터가 많아질수록 `OFFSET N`이 전체 행을 스캔하여 성능이 O(n)으로 저하

**개선**:
- `WHERE id < :cursor ORDER BY id DESC LIMIT :size` 방식의 커서 기반 페이지네이션 적용
- 어떤 페이지든 일정한 O(1) 성능 보장

**포인트**:
- 100만 건 이상의 게시글 데이터에서도 일관된 응답 속도 유지
- 무한 스크롤 UI와의 자연스러운 연동

---

## 3. JPQL Constructor Expression으로 DTO 직접 매핑

**문제**: Entity 전체를 조회한 뒤 Java에서 DTO로 변환하면 불필요한 컬럼까지 SELECT되고, N+1 문제 발생 위험

**개선**:
- JPQL `SELECT new ...Response(...)` 구문으로 DB 레벨에서 바로 DTO 생성
- 필요한 컬럼만 SELECT하여 네트워크/메모리 비용 절감
- `LEFT JOIN FETCH`와 조합하여 N+1 문제 방지

**포인트**:
- Entity → DTO 변환 레이어 제거로 코드 단순화
- DB에서 필요한 데이터만 가져오는 효율적 쿼리 설계

---

## 4. 이미지 저장소 전략 패턴 (Strategy Pattern)

**문제**: 개발 환경에서는 로컬 파일 시스템, 운영 환경에서는 AWS S3를 사용해야 하는데, 코드가 특정 저장소에 강결합

**개선**:
- `ImageManager` 인터페이스 + 다중 구현체 (S3ImageManager, LocalImageManager, ApiGatewayImageManager)
- 프로필에 따라 자동 빈 선택
- 파일 검증: MIME 타입(jpg/png/gif/webp), 크기(5MB), 개수(5개) 제한

**포인트**:
- 환경별 저장소 교체가 설정 한 줄로 가능
- 확장성: GCS, Azure Blob 등 새 저장소 추가 시 구현체만 추가

---

## 5. 구조화 로깅 + MDC 기반 요청 추적

**문제**: 운영 환경에서 특정 요청의 흐름을 추적하기 어렵고, 텍스트 로그는 검색/집계가 비효율적

**개선**:
- `MDCFilter`로 요청마다 고유 Request ID, HTTP Method, URI, Client IP를 MDC에 주입
- 운영 환경: JSON 포맷 로그 (Logback JSON encoder) → PLG 스택(Loki/Grafana) 연동
- 로컬 환경: 컬러 콘솔 출력으로 가독성 확보
- 로그 로테이션: 14일 보관, 총 1GB 상한

**포인트**:
- Request ID로 단일 요청의 전체 흐름을 엔드투엔드 추적 가능
- 구조화 로그로 Grafana에서 쿼리/대시보드 구성 용이

---

## 6. AOP 기반 API 로깅

**문제**: 모든 컨트롤러에 로깅 코드를 직접 넣으면 중복이 심하고, 비즈니스 로직과 관심사가 혼합

**개선**:
- `@Aspect`로 컨트롤러 메서드 진입/반환을 자동 로깅
- 요청 파라미터, 응답 상태, 실행 시간을 일관되게 기록
- 비즈니스 코드에는 로깅 코드 zero

**포인트**:
- 횡단 관심사(Cross-cutting Concern) 분리로 SRP 준수
- 새 API 추가 시 로깅 코드 작성 불필요

---

## 7. 글로벌 예외 처리 표준화

**문제**: 예외 발생 시 응답 형식이 일관되지 않으면 프론트엔드에서 에러 핸들링이 복잡해짐

**개선**:
- `ErrorCode` enum으로 에러 코드/메시지/HTTP 상태를 중앙 관리
- `CustomException` + `@RestControllerAdvice`(`GlobalExceptionHandler`)로 모든 예외를 `ApiResponse` 포맷으로 통일
- 예외 종류별 적절한 HTTP 상태 코드 반환

**포인트**:
- 프론트엔드와의 에러 응답 계약(Contract) 명확화
- 새 에러 추가 시 `ErrorCode`에 항목만 추가하면 됨

---

## 8. Soft Delete 패턴

**문제**: 물리 삭제(Hard Delete) 시 데이터 복구 불가, 참조 무결성 깨짐 위험

**개선**:
- User, Post, Comment에 `deleted` 플래그 + `deletedAt` 타임스탬프 적용
- 조회 쿼리에서 `WHERE deleted = false` 조건으로 논리적 삭제 구현
- 사용자 탈퇴 시 관련 게시글/댓글 cascade 논리 삭제

**포인트**:
- 데이터 감사(Audit) 및 복구 가능
- 통계/분석 시 삭제된 데이터도 활용 가능

---

## 9. Native Query 기반 원자적 카운터 업데이트

**문제**: 좋아요/조회수 카운트를 Java에서 읽고 +1 후 저장하면, 동시 요청 시 Lost Update 발생

**개선**:
- `UPDATE post_statuses SET like_count = like_count + 1 WHERE post_id = ?` 네이티브 쿼리 사용
- DB 레벨에서 원자적(atomic) 증감 보장
- JPA dirty checking 오버헤드 제거

**포인트**:
- Race Condition 없는 정확한 카운트 유지
- 별도 락(Lock) 없이 동시성 처리

---

## 10. UUID Binary(16) 저장 최적화

**문제**: UUID를 `CHAR(36)`으로 저장하면 인덱스 크기 2배 이상, 조회 성능 저하

**개선**:
- `@JdbcTypeCode(SqlTypes.BINARY)` + `columnDefinition = "binary(16)"` 적용
- 36바이트 → 16바이트로 저장 공간 57% 절감

**포인트**:
- 인덱스 크기 감소로 B-Tree 탐색 효율 향상
- 대용량 데이터에서 JOIN/WHERE 성능 개선

---

## 11. CI/CD 파이프라인 자동화

**문제**: 수동 배포는 휴먼 에러 발생 가능, 배포 주기가 길어짐

**개선**:
- GitHub Actions 기반 CI/CD 파이프라인 구축
- **CI**: 테스트 실행 → JaCoCo 커버리지 검증(70% 이상) → SpotBugs 정적 분석
- **CD**: Docker 이미지 빌드 → AWS ECR 푸시 → EC2 SSM 배포
- Alpine JRE 기반 경량 Docker 이미지

**포인트**:
- main 브랜치 push 시 자동 배포 (코드 커버리지/보안 검사 통과 필수)
- 정적 분석(SpotBugs + Find Security Bugs)으로 보안 취약점 사전 탐지

---

## 12. 대용량 부하 테스트 데이터 생성기

**문제**: 성능 테스트를 위해 현실적인 대량 데이터가 필요하지만, 수작업으로 만들기 비현실적

**개선**:
- `DataGenerator`로 JDBC 배치 인서트(배치 크기 5,000) 기반 대량 데이터 생성
- 생성 규모: 사용자 1만 / 게시글 100만 / 댓글 200만 / 좋아요 500만
- `rewriteBatchedStatements=true`로 MySQL 배치 최적화

**포인트**:
- 실 서비스 규모의 데이터셋으로 현실적인 성능 측정 가능
- 5~10GB 데이터 10~30분 내 생성

---

## 13. Prometheus + Grafana 모니터링 체계

**문제**: 운영 중 성능 병목이나 장애를 사후에야 인지

**개선**:
- Spring Boot Actuator + Micrometer Prometheus Registry 적용
- HikariCP 커넥션 풀, JVM GC, HTTP 요청 지표 자동 수집
- Alloy 기반 로그/메트릭 통합 수집 → Grafana 대시보드 시각화

**포인트**:
- p50/p95/p99 레이턴시, RPS, 에러율 실시간 모니터링
- DB 커넥션 풀 상태 감시로 병목 사전 탐지

---

## 14. getReferenceById() 프록시 활용

**문제**: 연관 엔티티 저장 시 `findById()`로 불필요한 SELECT 쿼리 발생

**개선**:
- FK만 필요한 경우 `getReferenceById()`로 프록시 객체 사용
- 실제 DB 조회 없이 연관관계 설정 가능

**포인트**:
- 불필요한 SELECT 쿼리 제거로 DB 부하 감소
- 게시글/댓글 생성 시 사용자 조회 쿼리 1개 절약

---

## 15. 테스트 코드 & 코드 품질 관리

**문제**: 리팩토링 시 기존 기능 깨짐을 감지하기 어렵고, 보안 취약점이 코드 리뷰에서 누락될 수 있음

**개선**:
- JUnit 5 + Mockito 기반 단위 테스트 (서비스 레이어 전체 커버)
- JaCoCo 70% 라인 커버리지 빌드 게이트
- SpotBugs + Find Security Bugs 플러그인으로 보안 정적 분석
- `@DisplayName` + Nested 테스트로 가독성 높은 테스트 구조

**포인트**:
- 커버리지 미달 시 빌드 실패 → 테스트 없는 코드 배포 방지
- 보안 취약점 자동 탐지로 OWASP Top 10 대응

---

## 향후 개선 예정 (TODO)

| 항목 | 기대 효과 |
|---|---|
| PostLike 유니크 제약 추가 | 동시 좋아요 중복 완전 방지 |
| Redis 캐싱 (인기글 Top10) | DB 부하 대폭 감소, 응답 속도 개선 |
| 조회수 Redis Write-Behind 집계 | 조회수 UPDATE 쿼리 90%+ 감소 |
| S3 업로드 트랜잭션 분리 | DB 커넥션 점유 시간 단축 |
| Bean Validation 적용 | 요청 검증 표준화, 보안 강화 |
| API Rate Limiting | 인증 API 브루트포스 공격 방어 |

---

> 각 개선 항목의 실측 수치는 `PERFORMANCE_OPTIMIZATION_GUIDE.md` 절차에 따라 기준선 측정 후 기록 예정
