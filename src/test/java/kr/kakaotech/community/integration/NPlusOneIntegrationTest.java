package kr.kakaotech.community.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import kr.kakaotech.community.auth.jwt.JwtProvider;
import kr.kakaotech.community.entity.Comment;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.PostStatus;
import kr.kakaotech.community.entity.PostType;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CommentRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N+1 감지용 통합 테스트.
 *
 * <p>각 테스트는 MockMvc로 실제 API 엔드포인트를 호출하고, 응답 헤더 {@code X-Query-Count}에서
 * 해당 요청 동안 실행된 SQL 쿼리 수를 읽어 하드 단언한다. 임계값은 "데이터 건수와 무관하게
 * 일정 수준을 넘지 않아야 한다"는 N+1 부재 조건으로 설정한다.
 *
 * <p>{@code @Transactional}로 테스트 자동 롤백 — seed 데이터는 테스트 범위에서만 살아있다가
 * 종료 시 사라진다. 서비스의 {@code @Transactional(readOnly=true)}는 테스트 트랜잭션에
 * (propagation REQUIRED) 참여하므로 MockMvc 요청이 seed 데이터를 볼 수 있다.
 */
@Transactional
class NPlusOneIntegrationTest extends NPlusOneTestSupport {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostStatusRepository postStatusRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired RedisTemplate<String, Object> redisTemplate;
    @Autowired JwtProvider jwtProvider;
    @PersistenceContext EntityManager em;

    private Cookie authCookie;

    private static final String TOP10_CACHE_KEY = "posts:top10";
    private static final int COMMENT_SEED_COUNT = 5;

    private int targetPostId;

    @BeforeEach
    void seed() {
        // Top10 캐시 flush (미스 경로 보장)
        redisTemplate.delete(TOP10_CACHE_KEY);

        // 닉네임은 12자 제한, UUID 앞 8자 사용 ("np1_" + 8자 = 12자)
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User author = new User(
                "nplus1-" + suffix + "@test.com",
                "pw",
                "np1_" + suffix,
                "USER"
        );
        userRepository.saveAndFlush(author);

        String accessToken = jwtProvider.createAccess(author.getId().toString(), "USER");
        authCookie = new Cookie("accessToken", accessToken);

        List<Integer> seededPostIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Post post = new Post(
                    "N+1 테스트 게시글 " + i,
                    "content-" + i,
                    PostType.COMPLETED,
                    author.getNickname(),
                    LocalDateTime.now().minusMinutes(i),
                    false,
                    author
            );
            postRepository.saveAndFlush(post);

            postStatusRepository.saveAndFlush(new PostStatus(post));
            seededPostIds.add(post.getId());
        }

        targetPostId = seededPostIds.get(0);

        Post targetPost = postRepository.findById(targetPostId).orElseThrow();
        // N+1 감지를 위해 댓글 작성자를 모두 다르게 생성한다. 같은 유저로 seed하면 Hibernate
        // 1차 캐시가 lazy user 로딩을 덮어 N+1이 가려진다.
        for (int i = 0; i < COMMENT_SEED_COUNT; i++) {
            String s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            User commenter = new User("c-" + s + "@test.com", "pw", "c_" + s, "USER");
            userRepository.saveAndFlush(commenter);
            commentRepository.saveAndFlush(new Comment("댓글 " + i, commenter, targetPost));
        }
    }

    private int callAndGetQueryCount(String url) throws Exception {
        // @Transactional 테스트에서 seed 엔티티가 persistence context(L1 cache)에 남아 있으면
        // lazy 관계도 캐시 히트로 처리되어 N+1이 가려진다. MockMvc 요청 전 flush + clear로
        // 실제 콜드 상태에서 쿼리 수를 측정한다.
        em.flush();
        em.clear();

        // AuthFilter가 /api 프리픽스 기준으로 인증 예외 경로를 매칭하므로,
        // requestURI는 /api/... 로 유지하고 servlet dispatch는 /posts 경로로 되도록 contextPath 사용.
        // 인증 필요한 엔드포인트 대비 JWT 쿠키도 항상 동봉.
        MvcResult result = mockMvc.perform(get("/api" + url).contextPath("/api").cookie(authCookie)).andReturn();
        String header = result.getResponse().getHeader(NPlusOneTestSupport.QUERY_COUNT_HEADER);
        assertThat(header)
                .as("테스트 필터가 X-Query-Count 헤더를 설정해야 한다")
                .isNotNull();
        int count = Integer.parseInt(header);
        System.out.println("[QUERY-COUNT] " + url + " → " + count + " queries (status=" + result.getResponse().getStatus() + ")");
        return count;
    }

    @Test
    @DisplayName("GET /posts — 게시글 목록 조회는 최대 2 쿼리")
    void getPostList_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts?size=5");

        assertThat(count)
                .as("projection 쿼리 단일 실행 기대. 초과 시 N+1 의심")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /posts/top10 — 인기글 조회(캐시 미스)는 최대 2 쿼리")
    void getPostTop10_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts/top10");

        assertThat(count)
                .as("Redis 캐시 미스 시에도 단일 projection 쿼리로 충분해야 함")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /posts/index — 홈 인덱스 목록은 최대 2 쿼리")
    void getPostIndex_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts/index");

        assertThat(count)
                .as("이미지 url 포함 projection 단일 쿼리 기대")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /posts/{id} — 상세 조회는 최대 2 쿼리")
    void getPostDetail_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts/" + targetPostId);

        assertThat(count)
                .as("JOIN FETCH로 images/user 한번에 로딩 기대")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /posts/{id}/statuses — 통계 조회는 최대 2 쿼리")
    void getPostStatuses_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts/" + targetPostId + "/statuses");

        assertThat(count)
                .as("findById 단일 쿼리 기대")
                .isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("GET /posts/{id}/comments — 댓글 목록은 댓글 수와 무관하게 최대 4 쿼리")
    void getCommentList_nPlusOne() throws Exception {
        int count = callAndGetQueryCount("/posts/" + targetPostId + "/comments");

        // 현재 구현: findById(post) + count(comments) + find comments + N x lazy user → 3+N 쿼리
        // 기대: user fetch join 적용 시 3~4 쿼리 고정 (댓글 수 증가와 무관해야 N+1 부재)
        assertThat(count)
                .as("댓글 수가 늘어도 쿼리 수가 증가하면 안 됨 (user fetch join 필요)")
                .isLessThanOrEqualTo(4);
    }
}
