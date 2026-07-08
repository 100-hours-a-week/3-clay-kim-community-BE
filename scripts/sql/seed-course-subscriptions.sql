-- MySQL 8.x
-- 코스 알림 부하 테스트용 사용자, 코스, 구독 데이터를 생성한다.
--
-- course_subscriptions에는 (user_id, course_id) UNIQUE 제약이 있으므로
-- 구독자 100,000명인 코스를 만들려면 서로 다른 사용자도 최소 100,000명 필요하다.
-- 요청한 기본 seed 사용자 수는 10,000명이지만, 아래 SQL은 가장 큰 구독자 수에 맞춰
-- 전용 seed 사용자를 100,000명까지 자동 확장한다.
--
-- 기존 데이터는 삭제하지 않는다. email, nickname, 구독 관계가 이미 있으면 INSERT IGNORE로 건너뛴다.

-- 운영 테이블과 임시 테이블의 문자열 비교 collation을 동일하게 맞춘다.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @requested_user_count := 10000;
SET @seed_email_domain := '@course-seed.test';

DROP TEMPORARY TABLE IF EXISTS seed_course_targets;
CREATE TEMPORARY TABLE seed_course_targets (
    course_name  VARCHAR(255) NOT NULL PRIMARY KEY,
    target_count INT NOT NULL
);

INSERT INTO seed_course_targets (course_name, target_count)
VALUES ('부하테스트 코스 100명', 100),
       ('부하테스트 코스 1천명', 1000),
       ('부하테스트 코스 1만명', 10000),
       ('부하테스트 코스 5만명', 50000),
       ('부하테스트 코스 10만명', 100000);

SET @max_subscription_count := (
    SELECT MAX(target_count)
    FROM seed_course_targets
);
SET @actual_user_count := GREATEST(@requested_user_count, @max_subscription_count);

-- 재귀 CTE 제한에 영향받지 않고 0부터 99,999까지 생성한다.
DROP TEMPORARY TABLE IF EXISTS seed_numbers;
CREATE TEMPORARY TABLE seed_numbers AS
SELECT ones.n
         + tens.n * 10
         + hundreds.n * 100
         + thousands.n * 1000
         + ten_thousands.n * 10000 AS n
FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) thousands
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ten_thousands;

ALTER TABLE seed_numbers ADD PRIMARY KEY (n);

-- 이 사용자는 알림 부하 테스트 전용이며 로그인 용도가 아니다.
INSERT IGNORE INTO users (
    id,
    email,
    password,
    nickname,
    deleted,
    created_at,
    deleted_at,
    role,
    image_id
)
SELECT UUID_TO_BIN(UUID()),
       CONCAT('course-seed-', LPAD(n + 1, 6, '0'), @seed_email_domain),
       'LOAD_TEST_USER_NOT_FOR_LOGIN',
       CONCAT('시드', LPAD(n + 1, 8, '0')),
       b'0',
       NOW(6),
       NULL,
       'USER',
       NULL
FROM seed_numbers
WHERE n < @actual_user_count
ORDER BY n;

INSERT INTO courses (name, current_status, created_at, updated_at)
SELECT target.course_name,
       'NORMAL',
       NOW(6),
       NULL
FROM seed_course_targets target
WHERE NOT EXISTS (
    SELECT 1
    FROM courses course
    WHERE course.name = target.course_name
);

-- 각 코스는 번호가 낮은 seed 사용자부터 목표 인원만큼 구독한다.
INSERT IGNORE INTO course_subscriptions (user_id, course_id, created_at)
SELECT seed_user.id,
       course.id,
       NOW(6)
FROM seed_course_targets target
JOIN courses course
  ON course.name = target.course_name
JOIN seed_numbers number
  ON number.n < target.target_count
JOIN users seed_user
  ON seed_user.email = CONCAT(
      'course-seed-',
      LPAD(number.n + 1, 6, '0'),
      @seed_email_domain
  );

-- 생성 결과 검증
SELECT @requested_user_count AS requested_seed_users,
       @actual_user_count AS actual_seed_users,
       COUNT(*) AS persisted_seed_users
FROM users
WHERE email LIKE CONCAT('course-seed-%', @seed_email_domain);

SELECT course.id,
       course.name,
       target.target_count,
       COUNT(subscription.id) AS actual_subscription_count
FROM seed_course_targets target
JOIN courses course
  ON course.name = target.course_name
LEFT JOIN course_subscriptions subscription
  ON subscription.course_id = course.id
GROUP BY course.id, course.name, target.target_count
ORDER BY target.target_count;

DROP TEMPORARY TABLE IF EXISTS seed_numbers;
DROP TEMPORARY TABLE IF EXISTS seed_course_targets;
