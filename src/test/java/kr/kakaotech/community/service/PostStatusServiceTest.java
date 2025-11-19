package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.repository.PostStatusRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 실제 DB를 사용한 동시성 테스트
 * 테스트 데이터는 자동으로 생성되고 정리됩니다.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostStatusServiceTest {

    @Autowired
    private PostStatusService postStatusService;

    @Autowired
    private PostStatusRepository postStatusRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer testPostId;

    @BeforeAll
    void setUpAll() {
        // 기존 테스트 데이터 정리
        cleanupTestData();

        // 새 테스트 데이터 생성
        createTestData();
    }

    @AfterAll
    void tearDownAll() {
        cleanupTestData();
    }

    private void createTestData() {
        // User 생성
        jdbcTemplate.update(
                "INSERT INTO users (id, email, nickname, password, role, created_at, deleted) " +
                        "VALUES (UNHEX(REPLACE(UUID(), '-', '')), 'concurrency_test@test.com', 'conctest', 'password', 'USER', NOW(), false)"
        );

        // Post 생성
        jdbcTemplate.update(
                "INSERT INTO posts (title, content, nickname, created_at, deleted, user_id) " +
                        "VALUES ('동시성 테스트용 게시글', '내용', 'conctest', NOW(), false, " +
                        "(SELECT id FROM users WHERE email = 'concurrency_test@test.com'))"
        );

        // 생성된 Post ID 조회
        this.testPostId = jdbcTemplate.queryForObject(
                "SELECT id FROM posts WHERE nickname = 'conctest' ORDER BY id DESC LIMIT 1",
                Integer.class
        );

        // PostStatus 생성
        jdbcTemplate.update(
                "INSERT INTO post_statuses (post_id, view_count, like_count, comment_count) " +
                        "VALUES (?, 0, 0, 0)",
                testPostId
        );

        System.out.println("테스트용 Post ID: " + testPostId + " 생성 완료");
    }

    private void cleanupTestData() {
        try {
            jdbcTemplate.update("DELETE FROM post_statuses WHERE post_id IN (SELECT id FROM posts WHERE nickname = 'conctest')");
            jdbcTemplate.update("DELETE FROM posts WHERE nickname = 'conctest'");
            jdbcTemplate.update("DELETE FROM users WHERE email = 'concurrency_test@test.com'");
        } catch (Exception e) {
            // 무시 (데이터가 없을 수도 있음)
        }
    }

    @Test
    @DisplayName("조회수 100번 동시 요청 시 정확히 100이 증가해야 한다")
    void concurrentViewCountTest() throws InterruptedException {
        // given
        // 테스트 시작 전 현재 조회수 확인
        PostStatus beforeStatus = postStatusRepository.findById(testPostId)
                .orElseThrow(() -> new RuntimeException("테스트용 Post ID " + testPostId + "가 DB에 존재하지 않습니다."));

        int initialViewCount = beforeStatus.getViewCount();
        int threadCount = 1000;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postStatusService.incrementViewCountRDB(testPostId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 짧은 대기 후 DB에서 다시 조회 (flush 보장)
        Thread.sleep(100);

        // then
        PostStatus afterStatus = postStatusRepository.findById(testPostId)
                .orElseThrow();

        int expectedViewCount = initialViewCount + threadCount;
        int actualViewCount = afterStatus.getViewCount();

        System.out.println("Initial: " + initialViewCount + ", Expected: " + expectedViewCount + ", Actual: " + actualViewCount);
        System.out.println("Lost updates: " + (expectedViewCount - actualViewCount));

        // 실제 동시성 문제 확인을 위해 실패하는 경우 로그 출력
        if (actualViewCount != expectedViewCount) {
            System.err.println("동시성 문제 발생! " + (expectedViewCount - actualViewCount) + "개의 업데이트가 손실되었습니다.");
        }

        assertThat(actualViewCount).isEqualTo(expectedViewCount);
    }
}