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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * </pre>
 *
 * <p><b>실측 결과 (Before 비관적 락)</b>:
 * <ul>
 *   <li>정합성({@code like_count == row 수})은 유지된다. 원인: Hibernate가 managed 엔티티
 *       {@code delete()} 시 flush 시점에 자동 stale detection을 수행하기 때문.
 *       한 스레드가 먼저 DELETE를 커밋하면 나머지는 {@code StaleObjectStateException}으로 롤백된다.
 *       {@code @Modifying} 쿼리({@code decrementLikeCount})가 auto-flush를 트리거하므로
 *       DELETE가 먼저 실행되고, stale이면 UPDATE는 아예 실행되지 않는다.</li>
 *   <li><b>가용성이 깨진다</b>: 100개 스레드 중 1개만 성공, 99개는 500 에러.
 *       사용자 입장에서는 "동시 클릭 시 대부분 실패"하는 심각한 UX 문제.</li>
 * </ul>
 *
 * <p>즉 이 버그는 "정합성 깨짐"이 아니라 <b>"가용성 깨짐"</b>으로 재정의되어야 한다.
 * 비관적 락을 적용하면 요청이 직렬화되어 모든 요청이 성공하게 된다.
 *
 * <p>주의:
 * <ul>
 *   <li>테스트 클래스에 {@code @Transactional}을 붙이면 모든 스레드가 단일 트랜잭션을 공유하게 되어
 *       동시성이 사라진다. 따라서 사용하지 않고 {@link #tearDown()}에서 수동 cleanup한다.</li>
 * </ul>
 */
@SpringBootTest
class LikeConcurrencyTest {

    @Autowired LikeService likeService;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostStatusRepository postStatusRepository;
    @Autowired LikeRepository likeRepository;
    @Autowired PlatformTransactionManager txManager;

    private UUID userId;
    private int postId;

    @BeforeEach
    void setUp() {
        // setUp 전체를 하나의 트랜잭션으로 묶어야 @MapsId 관계가 managed 상태에서 persist된다.
        // (repository.save()를 연달아 호출하면 각각 별도 트랜잭션이 되어 detached 참조 문제 발생)
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(s -> {
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
        });

        // 초기 상태: 좋아요 1개 등록 (별도 트랜잭션)
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
    @DisplayName("시나리오 2: 동시 더블 취소 시 모든 요청이 성공하고 정합성이 유지되어야 한다")
    void 동시_더블_취소_가용성과_정합성() throws InterruptedException {
        // given: setUp에서 좋아요 1개가 이미 등록된 상태
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        // 예외 종류별 카운트 + 첫 예외 메시지 샘플링
        Map<String, AtomicInteger> exceptionCounts = new ConcurrentHashMap<>();
        CopyOnWriteArrayList<String> exceptionSamples = new CopyOnWriteArrayList<>();

        // when: N개 스레드가 동시에 toggleLike 호출 (모두 취소 경로 진입 예상)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    likeService.toggleLike(userId, postId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    String type = e.getClass().getName();
                    exceptionCounts
                            .computeIfAbsent(type, k -> new AtomicInteger())
                            .incrementAndGet();
                    if (exceptionSamples.size() < 3) {
                        Throwable root = e;
                        while (root.getCause() != null) root = root.getCause();
                        exceptionSamples.add(type + " -> " + root.getClass().getSimpleName()
                                + ": " + root.getMessage());
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 출발
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then: like_count와 실제 post_likes row 수가 일치해야 한다
        int actualRows = likeRepository.countByPost_Id(postId);
        int likeCount = postStatusRepository.findById(postId).orElseThrow().getLikeCount();

        System.out.println("[설정] 초기 상태 like_count=1, threadCount=" + threadCount
                + " (모두 동일 유저/게시글에 동시 toggleLike → 전원 취소 경로 진입)");
        System.out.println("[결과] successCount=" + successCount + ", failCount=" + failCount
                + " (가용성 = " + successCount.get() + "/" + threadCount + ")");
        System.out.println("[결과] actualRows=" + actualRows + ", likeCount=" + likeCount
                + " (정합성 " + (actualRows == likeCount ? "OK" : "FAIL") + ")");
        System.out.println("[예외 종류별] " + exceptionCounts);
        exceptionSamples.forEach(s -> System.out.println("[예외 샘플] " + s));

        // 1) 정합성: like_count와 실제 row 수 일치
        assertThat(likeCount)
                .as("like_count는 실제 post_likes row 수와 일치해야 한다")
                .isEqualTo(actualRows);

        // 2) 가용성: 모든 요청이 성공해야 한다 (비관적 락 없이는 실패 — 9개가 StaleObjectStateException)
        assertThat(successCount.get())
                .as("동시 toggleLike 요청은 모두 성공해야 한다 (가용성)")
                .isEqualTo(threadCount);
    }
}
