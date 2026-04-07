package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.entity.PostType;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.LikeRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LikeService.toggleLike() 의 동시성 문제를 재현/검증하는 통합 테스트.
 *
 * <p>핵심 시나리오: 시나리오 2 — 동시 더블 취소
 * <pre>
 *   초기 상태: userA가 postX에 좋아요를 누른 상태 (row 1개, like_count=1)
 *   N개 스레드가 동시에 toggleLike(userA, postX) 호출
 *   → 모두 "있음 → 취소" 경로로 진입
 *   → delete는 1번만 실제로 일어나지만 decrement가 N번 실행
 *   → like_count ≠ 실제 row 수 (정합성 깨짐)
 * </pre>
 *
 * <p>주의:
 * <ul>
 *   <li>테스트 클래스에 {@code @Transactional}을 붙이면 모든 스레드가 단일 트랜잭션을 공유하게 되어
 *       동시성이 사라진다. 따라서 사용하지 않고 {@link #tearDown()}에서 수동 cleanup한다.</li>
 *   <li>H2(MODE=MySQL)에서도 check-then-act race는 재현된다.</li>
 * </ul>
 */
@SpringBootTest
class LikeConcurrencyTest {

    @Autowired LikeService likeService;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostStatusRepository postStatusRepository;
    @Autowired LikeRepository likeRepository;

    private UUID userId;
    private int postId;

    @BeforeEach
    void setUp() {
        User user = new User("like-test@example.com", "password", "likeTester", "USER");
        userRepository.save(user);
        userId = user.getId();

        Post post = new Post(
                "동시성 테스트 게시글",
                "content",
                PostType.IN_PROGRESS,
                user.getNickname(),
                LocalDateTime.now(),
                false,
                user
        );
        postRepository.save(post);
        postId = post.getId();

        PostStatus status = new PostStatus(post);
        postStatusRepository.save(status);

        // 초기 상태: 좋아요 1개 등록
        likeService.toggleLike(userId, postId);
    }

    @AfterEach
    void tearDown() {
        likeRepository.deleteAll();
        postStatusRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("시나리오 2: 동시 더블 취소 시 like_count와 실제 row 수가 일치해야 한다")
    void 동시_더블_취소_정합성() throws InterruptedException {
        // given: setUp에서 좋아요 1개가 이미 등록된 상태
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when: N개 스레드가 동시에 toggleLike 호출 (모두 취소 경로 진입 예상)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    likeService.toggleLike(userId, postId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 출발
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: like_count와 실제 post_likes row 수가 일치해야 한다
        int actualRows = likeRepository.countByPost_Id(postId);
        int likeCount = postStatusRepository.findById(postId).orElseThrow().getLikeCount();

        System.out.println("[결과] successCount=" + successCount + ", failCount=" + failCount);
        System.out.println("[결과] actualRows=" + actualRows + ", likeCount=" + likeCount);

        assertThat(likeCount)
                .as("like_count는 실제 post_likes row 수와 일치해야 한다")
                .isEqualTo(actualRows);
    }
}
