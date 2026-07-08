# 코스 현황 상태 제보 및 알림 기능 설계

## 1. 문제 정의

종주메이트는 국토종주 경험을 공유하는 커뮤니티지만, 현재 백엔드 도메인은 게시글, 댓글, 좋아요 중심의 범용 게시판에 가깝다.

국토종주 사용자는 단순한 최신글보다 다음 정보가 더 중요하다.

- 내가 지나갈 코스가 현재 정상인지
- 공사, 통제, 위험 구간이 있는지
- 다른 사용자가 최근에 제보한 상태가 있는지
- 관심 있는 코스에 새로운 제보가 생겼을 때 알림을 받을 수 있는지

따라서 `코스 현황` 기능을 추가해 국토종주 도메인의 실제 문제를 백엔드 모델로 표현한다.

## 2. 기능 범위

프론트 상단 탭에 `코스 현황`을 추가한다.

```text
국토종주 기록 | 주간 인기 여정 | 완주 인증 TOP 10 | 코스 현황
```

MVP 범위는 다음으로 제한한다.

- 코스 목록 조회
- 코스별 현재 상태 표시
  - 정상
  - 주의
  - 공사
  - 통제
- 코스별 현재 제보 리스트 조회
- 상태 제보 버튼
- 알림받기 버튼
- 마이페이지에서 내가 저장한 코스 또는 알림받는 코스 조회

제외 범위:

- 지도 기반 위치 표시
- 코스 세부 구간 분리
- 실시간 푸시 알림
- 관리자 검수 시스템
- 검색 색인 연동

위 항목은 MVP 이후 확장 대상으로 둔다.

## 3. 설계 의도

상태 제보는 사용자가 요청한 시점에 DB 저장은 즉시 완료되어야 하지만, 알림 생성은 요청 트랜잭션 안에서 처리할 필요가 없다.

특히 특정 코스를 알림받는 사용자가 많아질수록 제보 등록 요청 안에서 모든 알림을 생성하면 다음 문제가 생긴다.

- 제보 등록 API 응답 지연
- DB 커넥션 점유 시간 증가
- 알림 생성 실패가 제보 등록 실패로 전파
- 알림 대상자가 증가할수록 요청 처리 시간이 선형 증가

따라서 제보 저장과 알림 생성을 분리한다.

```text
제보 등록 API
→ CourseReport 저장
→ ReportCreatedEvent 발행
→ MQ
→ Consumer가 구독자 알림 생성
```

이 구조를 통해 사용자 요청은 빠르게 끝내고, 알림 생성은 비동기 재시도 가능한 작업으로 분리한다.

## 4. 도메인 모델

### Course

국토종주 코스 단위.

```text
id
name
description
current_status
created_at
updated_at
```

`current_status` 후보:

```text
NORMAL
CAUTION
CONSTRUCTION
CLOSED
```

### CourseReport

사용자가 등록한 코스 상태 제보.

```text
id
course_id
user_id
type
content
status
created_at
updated_at
```

`type` 후보:

```text
NORMAL
CAUTION
CONSTRUCTION
CLOSED
```

`status` 후보:

```text
ACTIVE
RESOLVED
DELETED
```

### CourseSubscription

사용자가 알림받거나 저장한 코스.

```text
id
course_id
user_id
created_at
```

제약 조건:

```text
unique(user_id, course_id)
```

같은 사용자가 같은 코스를 중복 저장하거나 중복 알림받지 않도록 막는다.

### Notification

제보 이벤트로 생성된 사용자 알림.

```text
id
user_id
course_report_id
event_id
title
content
read
created_at
```

제약 조건:

```text
unique(event_id, user_id)
```

Consumer 재시도 시 같은 사용자에게 같은 알림이 중복 생성되지 않도록 멱등성을 보장한다.

### Event Outbox

DB 저장과 이벤트 발행의 정합성을 보장하기 위한 Outbox 테이블.

```text
id
event_id
event_type
aggregate_type
aggregate_id
payload
status
created_at
published_at
```

`status` 후보:

```text
PENDING
PUBLISHED
FAILED
```

## 5. API 초안

### 코스 목록 조회

```http
GET /courses
```

응답 예시:

```json
{
  "message": "코스 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "name": "한강종주",
      "currentStatus": "NORMAL",
      "subscribed": true,
      "activeReportCount": 3
    }
  ]
}
```

### 코스별 제보 목록 조회

```http
GET /courses/{courseId}/reports
```

쿼리 파라미터:

```text
cursor
size
type
```

### 상태 제보 등록

```http
POST /courses/{courseId}/reports
```

요청 예시:

```json
{
  "type": "CONSTRUCTION",
  "content": "강변 진입로 일부 공사 중입니다. 우회가 필요합니다."
}
```

처리 흐름:

```text
CourseReport 저장
Course current_status 갱신
ReportCreatedEvent Outbox 저장
응답 반환
```

### 코스 알림받기

```http
POST /courses/{courseId}/subscription
```

### 코스 알림받기 취소

```http
DELETE /courses/{courseId}/subscription
```

### 내가 저장한 코스 조회

```http
GET /me/courses
```

### 내 알림 목록 조회

```http
GET /me/notifications
```

## 6. MQ 처리 흐름

### 이벤트 발행

제보 등록 트랜잭션 안에서는 MQ에 직접 발행하지 않고 Outbox에 이벤트를 저장한다.

```text
1. CourseReport 저장
2. Course current_status 갱신
3. EventOutbox 저장
4. 트랜잭션 커밋
5. Outbox publisher가 PENDING 이벤트를 MQ로 발행
6. 발행 성공 시 PUBLISHED로 변경
```

### Consumer 처리

```text
1. ReportCreatedEvent 수신
2. course_id 기준 CourseSubscription 조회
3. 구독자별 Notification 생성
4. unique(event_id, user_id)로 중복 생성 방지
5. 처리 실패 시 MQ 재시도
6. 반복 실패 시 DLQ로 이동
```

## 7. 기술 선택

### 1차 구현

AWS를 이미 사용하고 있으므로 `SQS + DLQ`를 우선 고려한다.

선택 이유:

- 운영 복잡도가 Kafka보다 낮음
- 알림 생성처럼 순서보다 재시도와 안정성이 중요한 작업에 적합
- DLQ 구성이 단순함
- 프로젝트 규모에서 도입 명분을 방어하기 쉬움

### Kafka를 바로 선택하지 않는 이유

Kafka는 고처리량 스트리밍, 파티션 기반 순서 보장, 여러 Consumer Group이 필요한 상황에서 강점이 있다.

현재 MVP의 핵심은 대규모 스트리밍보다 다음에 가깝다.

- 제보 등록 후 알림 생성
- 실패 재시도
- DLQ 격리
- 멱등 처리

따라서 MVP에서는 SQS가 더 현실적이다.

## 8. 성능 및 운영 기준

### API SLO 후보

```text
POST /courses/{courseId}/reports p95 < 300ms
GET /courses p95 < 200ms
GET /courses/{courseId}/reports p95 < 300ms
```

### MQ 처리 기준

```text
ReportCreatedEvent 정상 처리 지연 p95 < 5s
Notification 중복 생성 0건
DLQ 적재 이벤트 수 모니터링
Consumer 실패율 모니터링
```

### 주요 지표

- 제보 등록 API latency
- MQ publish 성공/실패 수
- Consumer 처리 성공/실패 수
- DLQ message count
- Notification 생성 수
- CourseSubscription 수
- DB connection active/pending

## 9. 검증 방법

### 기능 테스트

- 코스 목록 조회
- 제보 등록
- 제보 목록 조회
- 알림받기 등록/취소
- 내가 저장한 코스 조회

### 동시성 테스트

- 같은 사용자가 같은 코스를 동시에 알림받기 요청
  - `CourseSubscription`은 1건만 생성되어야 함
- 같은 이벤트가 Consumer에서 중복 처리
  - 같은 사용자에게 같은 `Notification`은 1건만 생성되어야 함

### 부하 테스트

시나리오:

```text
1. 1,000명의 사용자가 특정 코스를 알림받기 등록
2. 제보 1건 등록
3. 제보 등록 API 응답 시간 측정
4. 알림 1,000건 생성 완료 시간 측정
```

검증 목표:

- 제보 등록 API는 알림 대상자 수와 무관하게 빠르게 응답
- Consumer가 알림을 비동기로 생성
- 중복 알림 없음
- 실패 이벤트는 DLQ로 격리

## 10. 구현 단계

### Phase 1. 도메인 기본 기능

- Course 엔티티/API
- CourseReport 엔티티/API
- CourseSubscription 엔티티/API
- MyPage 저장 코스 조회 API

### Phase 2. 이벤트 구조

- ReportCreatedEvent 정의
- EventOutbox 엔티티 추가
- 제보 등록 시 Outbox 저장

### Phase 3. MQ 연동

- SQS 발행기 구현
- SQS Consumer 구현
- Notification 엔티티/API
- DLQ 설정

### Phase 4. 검증 및 문서화

- 동시성 테스트
- Consumer 멱등성 테스트
- k6 부하 테스트
- Grafana 지표 캡처
- 포트폴리오 개선 문서 작성

## 11. 프론트 MVP 방향

백엔드 API가 아직 없을 때도 프론트는 버튼만 놓는 것보다 화면 구조와 상태를 먼저 잡는 편이 좋다.

권장 범위:

- 상단 탭에 `코스 현황` 추가
- mock 데이터로 코스 목록 표시
- 코스 카드에 현재 상태 뱃지 표시
- `상태 제보` 버튼 추가
- `알림받기` 버튼 추가
- 제보 리스트 영역 추가
- 마이페이지에 `내가 저장한 코스` 섹션 추가

API가 없을 때의 처리:

- 버튼 클릭 시 모달 또는 토스트만 표시
- 실제 API 호출부는 adapter 함수로 분리
- 이후 백엔드 API가 생기면 mock adapter만 실제 HTTP client로 교체

예시:

```text
CourseStatusPage
CourseCard
CourseReportList
CourseReportButton
CourseSubscribeButton
MySavedCourses
```

## 12. 이력서/면접 설명 문장

```text
국토종주 커뮤니티에서 사용자가 실제로 필요로 하는 정보는 최신글보다 코스의 현재 상태라고 판단해, 코스 현황과 상태 제보 도메인을 추가했습니다.
제보 등록과 구독자 알림 생성을 같은 트랜잭션에서 처리하면 알림 대상자 수에 따라 응답 시간이 증가하므로, Outbox와 MQ를 이용해 제보 저장과 알림 생성을 분리했습니다.
Consumer 재시도 시 중복 알림이 생성되지 않도록 eventId와 userId 기반 유니크 제약으로 멱등성을 보장했고, 실패 이벤트는 DLQ로 격리해 운영자가 재처리할 수 있도록 설계했습니다.
```
