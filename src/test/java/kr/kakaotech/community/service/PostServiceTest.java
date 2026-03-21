package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.PostModifyRequest;
import kr.kakaotech.community.dto.request.PostRegisterRequest;
import kr.kakaotech.community.dto.response.*;
import kr.kakaotech.community.entity.*;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PostRepository postRepository;
    @Mock
    PostStatusRepository postStatusRepository;
    @Mock
    ImageService imageService;
    @Mock
    PostStatusService postStatusService;
    @Mock
    org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    @Mock
    org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    @InjectMocks
    PostService postService;

    private UUID userId;
    private User user;
    private Image userImage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userImage = new Image("https://img.com/profile.png");
        ReflectionTestUtils.setField(userImage, "id", 1);

        user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("encodedPassword")
                .nickname("테스터")
                .deleted(false)
                .role(UserRole.USER)
                .image(userImage)
                .build();

    }

    private Post createPost(int id, User user) {
        Post post = new Post("제목", "내용", PostType.IN_PROGRESS, user.getNickname(), LocalDateTime.now(), false, user);
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private PostSummaryResponse createSummary(int id) {
        return new PostSummaryResponse(id, "제목" + id, "테스터", LocalDateTime.now(), 0, 0, 0, null, PostType.IN_PROGRESS);
    }

    @Nested
    @DisplayName("게시글 등록 (registerPost)")
    class RegisterPost {

        @Test
        @DisplayName("성공 - 이미지 없이 등록")
        void success_noImage() {
            // given
            PostRegisterRequest request = new PostRegisterRequest("제목", "내용", null, "IN_PROGRESS");
            Post savedPost = createPost(1, user);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(postRepository.saveAndFlush(any(Post.class))).willReturn(savedPost);
            given(postStatusRepository.save(any(PostStatus.class))).willReturn(null);

            // when
            int postId = postService.registerPost(userId.toString(), request, null);

            // then
            assertThat(postId).isEqualTo(1);
            verify(postRepository).saveAndFlush(any(Post.class));
            verify(postStatusRepository).save(any(PostStatus.class));
            verify(imageService, never()).saveImage(anyList(), any(Post.class));
        }

        @Test
        @DisplayName("실패 - 이미지 6장 초과")
        void fail_tooManyImages() {
            // given
            PostRegisterRequest request = new PostRegisterRequest("제목", "내용", null, "IN_PROGRESS");
            List images = List.of(mock(), mock(), mock(), mock(), mock(), mock()); // 6장

            // when & then
            assertThatThrownBy(() -> postService.registerPost(userId.toString(), request, images))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.IMAGE_TOO_MANY));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void fail_userNotFound() {
            // given
            PostRegisterRequest request = new PostRegisterRequest("제목", "내용", null, "IN_PROGRESS");
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.registerPost(unknownId.toString(), request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
        }
    }

    @Nested
    @DisplayName("게시글 목록 조회 (getPostList)")
    class GetPostList {

        @Test
        @DisplayName("첫 조회 (cursor null) - 다음 페이지 있음")
        void firstPage_hasNext() {
            // given
            List<PostSummaryResponse> posts = List.of(createSummary(5), createSummary(4), createSummary(3));
            given(postRepository.findTopPost(any())).willReturn(posts);

            // when
            PostListResponse response = postService.getPostList(null, 3);

            // then
            assertThat(response.getPosts()).hasSize(3);
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getNextCursor()).isEqualTo(3);
            assertThat(response.getPosts().get(0).getImageUrl()).isNull();
            verify(postRepository).findTopPost(any());
            verify(postRepository, never()).findPostByCursor(anyInt(), any());
        }

        @Test
        @DisplayName("커서 조회 - 다음 페이지 없음")
        void withCursor_noNext() {
            // given
            List<PostSummaryResponse> posts = List.of(createSummary(2));
            given(postRepository.findPostByCursor(eq(3), any())).willReturn(posts);

            // when
            PostListResponse response = postService.getPostList(3, 3);

            // then
            assertThat(response.getPosts()).hasSize(1);
            assertThat(response.isHasNext()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("인기글 목록 (getLikePostList)")
    class GetLikePostList {

        @Test
        @DisplayName("성공 - daily 기간 필터")
        void success_daily() {
            // given
            List<PostSummaryResponse> posts = List.of(createSummary(1));
            given(postRepository.findPostByLikeCount(any(LocalDateTime.class), any())).willReturn(posts);

            // when
            PostListResponse response = postService.getLikePostList(null, "daily", 5);

            // then
            assertThat(response.getPosts()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - weekly 기간 필터")
        void success_weekly() {
            // given
            List<PostSummaryResponse> posts = List.of(createSummary(1), createSummary(2));
            given(postRepository.findPostByLikeCount(any(LocalDateTime.class), any())).willReturn(posts);

            // when
            PostListResponse response = postService.getLikePostList(null, "weekly", 5);

            // then
            assertThat(response.getPosts()).hasSize(2);
        }

        @Test
        @DisplayName("실패 - 잘못된 기간 필터")
        void fail_invalidPeriod() {
            // when & then
            assertThatThrownBy(() -> postService.getLikePostList(null, "monthly", 5))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_REQUEST_FILTER));
        }
    }

    @Nested
    @DisplayName("게시글 상세 조회 (getPostDetails)")
    class GetPostDetails {

        @Test
        @DisplayName("성공 - 이미지 포함")
        void success_withImages() {
            // given
            Post post = createPost(1, user);

            Image postImage = new Image("https://img.com/post1.png");
            ReflectionTestUtils.setField(postImage, "id", 10);
            PostImage pi = new PostImage(post, postImage);
            post.getPostImages().add(pi);

            given(postRepository.findPostDetailsWithImages(1)).willReturn(Optional.of(post));

            // when
            PostDetailResponse response = postService.getPostDetails(1);

            // then
            assertThat(response.getTitle()).isEqualTo("제목");
            assertThat(response.getNickname()).isEqualTo("테스터");
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getProfileImageUrl()).isEqualTo("https://img.com/profile.png");
            assertThat(response.getImages()).hasSize(1);
            assertThat(response.getImages().get(0).getImageUrl()).isEqualTo("https://img.com/post1.png");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글")
        void fail_notFound() {
            // given
            given(postRepository.findPostDetailsWithImages(999)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.getPostDetails(999))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_POST));
        }
    }

    @Nested
    @DisplayName("게시글 수정 (updatePost)")
    class UpdatePost {

        @Test
        @DisplayName("성공 - 제목/내용 수정")
        void success() {
            // given
            Post post = createPost(1, user);
            PostModifyRequest request = new PostModifyRequest("새제목", "새내용", "COMPLETED", null);

            given(postRepository.findById(1)).willReturn(Optional.of(post));

            // when
            postService.updatePost(1, userId.toString(), request, null);

            // then
            assertThat(post.getTitle()).isEqualTo("새제목");
            assertThat(post.getContent()).isEqualTo("새내용");
            assertThat(post.getType()).isEqualTo(PostType.COMPLETED);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글")
        void fail_notFound() {
            // given
            PostModifyRequest request = new PostModifyRequest("새제목", "새내용", "COMPLETED", null);
            given(postRepository.findById(999)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.updatePost(999, userId.toString(), request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_POST));
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 게시글")
        void fail_forbidden() {
            // given
            Post post = createPost(1, user);
            PostModifyRequest request = new PostModifyRequest("새제목", "새내용", "COMPLETED", null);
            String otherUserId = UUID.randomUUID().toString();

            given(postRepository.findById(1)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.updatePost(1, otherUserId, request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("게시글 삭제 (deletePost)")
    class DeletePost {

        @Test
        @DisplayName("성공 - soft delete")
        void success() {
            // given
            Post post = createPost(1, user);
            given(postRepository.findById(1)).willReturn(Optional.of(post));

            // when
            postService.deletePost(1, userId.toString());

            // then
            assertThat(post.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 게시글")
        void fail_notFound() {
            // given
            given(postRepository.findById(999)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> postService.deletePost(999, userId.toString()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_POST));
        }

        @Test
        @DisplayName("실패 - 이미 삭제된 게시글")
        void fail_alreadyDeleted() {
            // given
            Post post = createPost(1, user);
            post.deletePost(); // deleted = true
            given(postRepository.findById(1)).willReturn(Optional.of(post));

            // when & then
            assertThatThrownBy(() -> postService.deletePost(1, userId.toString()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_POST));
        }
    }

    @Nested
    @DisplayName("Top10 조회 (getPostTop10List)")
    class GetPostTop10 {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            List<PostSummaryResponse> posts = List.of(
                    createSummary(1), createSummary(2), createSummary(3)
            );
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("posts:top10")).willReturn(null);
            given(postRepository.findTop10Post(any(LocalDateTime.class), any())).willReturn(posts);

            // when
            PostListResponse response = postService.getPostTop10List();

            // then
            assertThat(response.getPosts()).hasSize(3);
            assertThat(response.isHasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("닉네임 검색 (getNicknamePostList)")
    class GetNicknamePostList {

        @Test
        @DisplayName("성공 - 닉네임으로 검색")
        void success() {
            // given
            List<PostSummaryResponse> posts = List.of(createSummary(1), createSummary(2));
            given(postRepository.findPostByNickname(eq("테스터"), any())).willReturn(posts);

            // when
            PostListResponse response = postService.getNicknamePostList(null, "테스터", 5);

            // then
            assertThat(response.getPosts()).hasSize(2);
        }
    }
}
