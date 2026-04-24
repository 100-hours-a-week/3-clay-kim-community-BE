package kr.kakaotech.community.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import kr.kakaotech.community.repository.LikeRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiFunctionalIntegrationTest {

    private static final String PASSWORD = "Aa123456!";
    private static final String NEW_PASSWORD = "Bb123456!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostStatusRepository postStatusRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired LikeRepository likeRepository;
    @Autowired JwtProvider jwtProvider;
    @Autowired PasswordEncoder passwordEncoder;
    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("POST /auth — 로그인 성공 시 토큰 쿠키를 발급한다")
    void login_success() throws Exception {
        User user = saveUser("lo");

        mockMvc.perform(post("/api/auth")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.userEmail").value(user.getEmail()));
    }

    @Test
    @DisplayName("POST /auth — 비밀번호가 틀리면 400을 반환한다")
    void login_fail_badPassword() throws Exception {
        User user = saveUser("lf");

        mockMvc.perform(post("/api/auth")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", "wrong"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /auth/refresh — refreshToken이 유효하면 새 토큰 쿠키를 발급한다")
    void refreshToken_success() throws Exception {
        User user = saveUser("rf");
        Cookie[] cookies = loginCookies(user);

        mockMvc.perform(get("/api/auth/refresh")
                        .contextPath("/api")
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("POST /auth/token — 로그아웃 성공 시 refreshToken을 폐기한다")
    void logout_success() throws Exception {
        User user = saveUser("lg");
        Cookie[] cookies = loginCookies(user);

        mockMvc.perform(post("/api/auth/token")
                        .contextPath("/api")
                        .cookie(cookies))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }

    @Test
    @DisplayName("POST /users — 회원가입 성공 시 사용자를 생성한다")
    void registerUser_success() throws Exception {
        String suffix = suffix();
        String email = "join-" + suffix + "@test.com";

        mockMvc.perform(multipart("/api/users")
                        .contextPath("/api")
                        .param("email", email)
                        .param("nickname", "jn" + suffix)
                        .param("password", PASSWORD)
                        .param("role", "USER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(email));

        flushAndClear();
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    @DisplayName("POST /users — 중복 이메일이면 409를 반환한다")
    void registerUser_fail_duplicateEmail() throws Exception {
        User user = saveUser("du");

        mockMvc.perform(multipart("/api/users")
                        .contextPath("/api")
                        .param("email", user.getEmail())
                        .param("nickname", "du" + suffix())
                        .param("password", PASSWORD)
                        .param("role", "USER"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /users/{id} — 본인 닉네임을 수정한다")
    void updateUser_success() throws Exception {
        User user = saveUser("uu");

        mockMvc.perform(patchMultipart("/api/users/{userId}", user.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(user))
                        .param("nickname", "newnick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("newnick"));

        flushAndClear();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getNickname()).isEqualTo("newnick");
    }

    @Test
    @DisplayName("PATCH /users/{id} — 다른 회원 수정은 403을 반환한다")
    void updateUser_fail_forbidden() throws Exception {
        User owner = saveUser("uo");
        User attacker = saveUser("ua");

        mockMvc.perform(patchMultipart("/api/users/{userId}", owner.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(attacker))
                        .param("nickname", "hacked"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /users/password — 비밀번호를 변경하고 토큰을 폐기한다")
    void changePassword_success() throws Exception {
        User user = saveUser("pw");
        Cookie[] cookies = loginCookies(user);

        mockMvc.perform(patch("/api/users/password")
                        .contextPath("/api")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(jsonPath("$.data").value(true));

        flushAndClear();
        String encodedPassword = userRepository.findById(user.getId()).orElseThrow().getPassword();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, encodedPassword)).isTrue();
    }

    @Test
    @DisplayName("PATCH /users/password — 현재 비밀번호가 틀리면 400을 반환한다")
    void changePassword_fail_badPassword() throws Exception {
        User user = saveUser("pf");

        mockMvc.perform(patch("/api/users/password")
                        .contextPath("/api")
                        .cookie(accessCookie(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "wrong", "newPassword", NEW_PASSWORD))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivation — 회원을 소프트 삭제한다")
    void deactivateUser_success() throws Exception {
        User user = saveUser("wd");
        Cookie[] cookies = loginCookies(user);

        mockMvc.perform(patch("/api/users/{userId}/deactivation", user.getId())
                        .contextPath("/api")
                        .cookie(cookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));

        flushAndClear();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getDeleted()).isTrue();
    }

    @Test
    @DisplayName("POST /posts — 게시글을 생성하고 PostStatus를 생성한다")
    void registerPost_success() throws Exception {
        User user = saveUser("po");

        MvcResult result = mockMvc.perform(multipart("/api/posts")
                        .contextPath("/api")
                        .cookie(accessCookie(user))
                        .param("title", "새 게시글")
                        .param("content", "내용")
                        .param("type", "IN_PROGRESS"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("게시글 등록 성공"))
                .andReturn();

        int postId = jsonNode(result).path("data").asInt();
        flushAndClear();
        assertThat(postRepository.findById(postId)).isPresent();
        assertThat(postStatusRepository.findById(postId)).isPresent();
    }

    @Test
    @DisplayName("POST /posts — 인증이 없으면 401을 반환한다")
    void registerPost_fail_unauthorized() throws Exception {
        mockMvc.perform(multipart("/api/posts")
                        .contextPath("/api")
                        .param("title", "새 게시글")
                        .param("content", "내용")
                        .param("type", "IN_PROGRESS"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /posts/{id} — 작성자는 게시글을 수정할 수 있다")
    void updatePost_success() throws Exception {
        User author = saveUser("pa");
        Post post = savePost(author);

        mockMvc.perform(patchMultipart("/api/posts/{postId}", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(author))
                        .param("title", "수정 제목")
                        .param("content", "수정 내용")
                        .param("type", "COMPLETED"))
                .andExpect(status().isOk());

        flushAndClear();
        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정 제목");
        assertThat(updated.getType()).isEqualTo(PostType.COMPLETED);
    }

    @Test
    @DisplayName("PATCH /posts/{id} — 작성자가 아니면 403을 반환한다")
    void updatePost_fail_forbidden() throws Exception {
        User author = saveUser("pb");
        User attacker = saveUser("pc");
        Post post = savePost(author);

        mockMvc.perform(patchMultipart("/api/posts/{postId}", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(attacker))
                        .param("title", "수정 제목")
                        .param("content", "수정 내용")
                        .param("type", "COMPLETED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /posts/{id}/deactivation — 작성자는 게시글을 삭제할 수 있다")
    void deactivatePost_success() throws Exception {
        User author = saveUser("pd");
        Post post = savePost(author);

        mockMvc.perform(patch("/api/posts/{postId}/deactivation", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(author)))
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(postRepository.findById(post.getId()).orElseThrow().getDeleted()).isTrue();
    }

    @Test
    @DisplayName("PATCH /posts/{id}/deactivation — 작성자가 아니면 403을 반환한다")
    void deactivatePost_fail_forbidden() throws Exception {
        User author = saveUser("pe");
        User attacker = saveUser("px");
        Post post = savePost(author);

        mockMvc.perform(patch("/api/posts/{postId}/deactivation", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(attacker)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /posts — 잘못된 기간 필터는 400을 반환한다")
    void getPosts_fail_invalidPeriod() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .contextPath("/api")
                        .param("period", "monthly"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /posts/{id}/comments — 댓글을 생성하고 댓글 수를 증가시킨다")
    void registerComment_success() throws Exception {
        User author = saveUser("ca");
        User commenter = saveUser("cb");
        Post post = savePost(author);
        long beforeCount = commentRepository.count();

        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(commenter))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "댓글입니다"))))
                .andExpect(status().isCreated());

        flushAndClear();
        assertThat(commentRepository.count()).isEqualTo(beforeCount + 1);
        assertThat(postStatusRepository.findById(post.getId()).orElseThrow().getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /posts/{id}/comments — 없는 게시글이면 404를 반환한다")
    void registerComment_fail_postNotFound() throws Exception {
        User user = saveUser("cf");

        mockMvc.perform(post("/api/posts/{postId}/comments", missingPostId())
                        .contextPath("/api")
                        .cookie(accessCookie(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "댓글입니다"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /comments/{id} — 작성자는 댓글을 수정할 수 있다")
    void updateComment_success() throws Exception {
        User author = saveUser("cc");
        Post post = savePost(author);
        Comment comment = saveComment(author, post);

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "수정 댓글"))))
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getContent()).isEqualTo("수정 댓글");
    }

    @Test
    @DisplayName("PATCH /comments/{id} — 작성자가 아니면 403을 반환한다")
    void updateComment_fail_forbidden() throws Exception {
        User author = saveUser("cd");
        User attacker = saveUser("ce");
        Post post = savePost(author);
        Comment comment = saveComment(author, post);

        mockMvc.perform(patch("/api/comments/{commentId}", comment.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "수정 댓글"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /comments/{id}/deactivation — 작성자는 댓글을 삭제할 수 있다")
    void deactivateComment_success() throws Exception {
        User author = saveUser("ci");
        Post post = savePost(author);
        Comment comment = saveComment(author, post);

        mockMvc.perform(patch("/api/comments/{commentId}/deactivation", comment.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(author)))
                .andExpect(status().isOk());

        flushAndClear();
        assertThat(commentRepository.findById(comment.getId()).orElseThrow().getDeleted()).isTrue();
        assertThat(postStatusRepository.findById(post.getId()).orElseThrow().getCommentCount()).isZero();
    }

    @Test
    @DisplayName("POST /posts/{id}/likes — 좋아요를 토글한다")
    void toggleLike_success() throws Exception {
        User author = saveUser("la");
        User user = saveUser("lb");
        Post post = savePost(author);

        mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeStatus").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                        .contextPath("/api")
                        .cookie(accessCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeStatus").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        flushAndClear();
        assertThat(likeRepository.countByPost_Id(post.getId())).isZero();
        assertThat(postStatusRepository.findById(post.getId()).orElseThrow().getLikeCount()).isZero();
    }

    @Test
    @DisplayName("POST /posts/{id}/likes — 인증이 없으면 401을 반환한다")
    void toggleLike_fail_unauthorized() throws Exception {
        User author = saveUser("lc");
        Post post = savePost(author);

        mockMvc.perform(post("/api/posts/{postId}/likes", post.getId())
                        .contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /posts/{id}/likes — 없는 게시글이면 404를 반환한다")
    void toggleLike_fail_postNotFound() throws Exception {
        User user = saveUser("ld");

        mockMvc.perform(post("/api/posts/{postId}/likes", missingPostId())
                        .contextPath("/api")
                        .cookie(accessCookie(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /posts/{id}/likes — 비로그인 사용자는 좋아요 상태 false를 받는다")
    void getLikeStatus_success_anonymous() throws Exception {
        User author = saveUser("le");
        Post post = savePost(author);

        mockMvc.perform(get("/api/posts/{postId}/likes", post.getId())
                        .contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeStatus").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    @DisplayName("POST /images — 이미지가 아닌 파일이면 400을 반환한다")
    void uploadImage_fail_badContentType() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "images",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not-image".getBytes()
        );

        mockMvc.perform(multipart("/api/images")
                        .file(textFile)
                        .contextPath("/api"))
                .andExpect(status().isBadRequest());
    }

    private User saveUser(String prefix) {
        String suffix = suffix();
        User user = new User(
                prefix + "-" + suffix + "@test.com",
                passwordEncoder.encode(PASSWORD),
                prefix + suffix,
                "USER"
        );
        return userRepository.saveAndFlush(user);
    }

    private Post savePost(User user) {
        Post post = new Post(
                "제목 " + suffix(),
                "내용",
                PostType.IN_PROGRESS,
                user.getNickname(),
                LocalDateTime.now(),
                false,
                user
        );
        Post savedPost = postRepository.saveAndFlush(post);
        postStatusRepository.saveAndFlush(new PostStatus(savedPost));
        return savedPost;
    }

    private Comment saveComment(User user, Post post) {
        Comment comment = commentRepository.saveAndFlush(new Comment("댓글", user, post));
        postStatusRepository.incrementCommentCount(post.getId());
        flushAndClear();
        return comment;
    }

    private Cookie accessCookie(User user) {
        Cookie cookie = new Cookie("accessToken", jwtProvider.createAccess(user.getId().toString(), "USER"));
        cookie.setPath("/");
        return cookie;
    }

    private Cookie[] loginCookies(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie accessToken = findCookie(result, "accessToken");
        Cookie refreshToken = findCookie(result, "refreshToken");
        return new Cookie[] {accessToken, refreshToken};
    }

    private Cookie findCookie(MvcResult result, String name) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .orElseThrow();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode jsonNode(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private MockMultipartHttpServletRequestBuilder patchMultipart(String urlTemplate, Object... uriVars) {
        MockMultipartHttpServletRequestBuilder builder = multipart(urlTemplate, uriVars);
        builder.with(patchMethod());
        return builder;
    }

    private RequestPostProcessor patchMethod() {
        return request -> {
            request.setMethod("PATCH");
            return request;
        };
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private String suffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private int missingPostId() {
        int candidate = Integer.MAX_VALUE;
        while (postRepository.existsById(candidate)) {
            candidate--;
        }
        return candidate;
    }
}
