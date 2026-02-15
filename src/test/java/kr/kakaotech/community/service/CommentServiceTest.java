package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.CommentRequest;
import kr.kakaotech.community.dto.response.CommentResponse;
import kr.kakaotech.community.entity.*;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CommentRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostStatusRepository postStatusRepository;

    @InjectMocks
    private CommentService commentService;

    // 테스트용 데이터
    private final UUID testUserId = UUID.randomUUID();
    private final int testPostId = 1;
    private final int testCommentId = 1;

    private Image createTestImage() {
        return new Image("test-url");
    }

    private User createTestUser() {
        User user = new User("test@test.com", "password", "testUser", "USER");
        user.addImage(createTestImage());
        ReflectionTestUtils.setField(user, "id", testUserId);

        return user;
    }

    private Post createTestPost(User user) {
        Post post = new Post("테스트 제목", "테스트 내용", PostType.IN_PROGRESS, "tester", LocalDateTime.now(), false, user);
        ReflectionTestUtils.setField(post, "id", testPostId);

        return post;
    }

    private Comment createTestComment(User user, Post post) {
        return new Comment("테스트 댓글", user, post);
    }

    @Test
    @DisplayName("댓글 등록 - 성공")
    void registerComment_success() {
        // given
        String userId = testUserId.toString();
        CommentRequest request = new CommentRequest();

        User user = createTestUser();
        Post post = createTestPost(user);

        given(userRepository.findById(testUserId)).willReturn(Optional.of(user));
        given(postRepository.findById(testPostId)).willReturn(Optional.of(post));

        // when
        commentService.registerComment(userId, testPostId, request);

        // then
        verify(userRepository, times(1)).findById(testUserId);
        verify(postRepository, times(1)).findById(testPostId);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(postStatusRepository, times(1)).incrementCommentCount(testPostId);
    }

    @Test
    @DisplayName("댓글 등록 - 사용자 없음 실패")
    void registerComment_userNotFound() {
        // given
        String userId = testUserId.toString();
        CommentRequest request = new CommentRequest();

        given(userRepository.findById(testUserId)).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> commentService.registerComment(userId, testPostId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NOT_FOUND_USER.getMessage());

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postStatusRepository, never()).incrementCommentCount(testPostId);
    }

    @Test
    @DisplayName("댓글 등록 - 게시글 없음 실패")
    void registerComment_postNotFound() {
        // given
        String userId = testUserId.toString();
        CommentRequest request = new CommentRequest();
        User user = createTestUser();

        given(userRepository.findById(testUserId)).willReturn(Optional.of(user));
        given(postRepository.findById(testPostId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.registerComment(userId, testPostId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NOT_FOUND_POST.getMessage());

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postStatusRepository, never()).incrementCommentCount(testPostId);
    }

    @Test
    @DisplayName("댓글 목록 조회 - 성공")
    void getCommentList_success() {
        // given
        User user = createTestUser();
        Post post = createTestPost(user);

        Comment comment1 = createTestComment(user, post);
        Comment comment2 = createTestComment(user, post);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> commentPage = new PageImpl<>(List.of(comment1, comment2));

        given(postRepository.findById(testPostId)).willReturn(Optional.of(post));
        given(commentRepository.findByPost(post, pageable)).willReturn(commentPage);

        // when
        Page<CommentResponse> result = commentService.getCommentList(post.getId(), pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(postRepository, times(1)).findById(testPostId);
        verify(commentRepository, times(1)).findByPost(post, pageable);
    }

    @Test
    @DisplayName("댓글 목록 조회 - 게시글 없음 실패")
    void getCommentList_postNotFound() {
        // given
        User user = createTestUser();
        Post post = createTestPost(user);
        Pageable pageable = PageRequest.of(0, 10);


        // when & then
        assertThatThrownBy(() -> commentService.getCommentList(testPostId, pageable))
                        .isInstanceOf(CustomException.class)
                        .hasMessageContaining(ErrorCode.NOT_FOUND_POST.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 - 성공")
    void updateComment_success() {
        // given
        String userId = testUserId.toString();
        int commentId = testCommentId;
        CommentRequest request = new CommentRequest();
        User user = createTestUser();
        Post post = createTestPost(user);
        Comment comment = new Comment("테스트입니다.", user, post);

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.updateComment(userId, commentId, request);

        // then
        verify(commentRepository, times(1)).findById(testCommentId);
    }

    @Test
    @DisplayName("댓글 수정 - 댓글 없음 실패")
    void updateComment_commentNotFound() {
        // given
        String userId = testUserId.toString();
        User user = createTestUser();
        Post post = createTestPost(user);

        int commentId = testCommentId;
        CommentRequest request = new CommentRequest();
        Comment comment = new Comment("테스트입니다.", user, post);

        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(userId, commentId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.NOT_FOUND_COMMENT.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 - 댓글과 유저 ID가 다름 실패")
    void updateComment_userNotFound() {
        // given
        User user = createTestUser();
        Post post = createTestPost(user);

        int commentId = testCommentId;
        CommentRequest request = new CommentRequest();
        Comment comment = new Comment("테스트입니다.", user, post);

        String wrongUserId = "wrongUserId";

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(wrongUserId, commentId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("댓글 수정 - 이미 삭제된 댓글 실패")
    void updateComment_commentNotDeleted() {
        // given
        String userId = testUserId.toString();
        User user = createTestUser();
        Post post = createTestPost(user);

        int commentId = testCommentId;
        CommentRequest request = new CommentRequest();
        Comment comment = new Comment("테스트입니다.", user, post);
        ReflectionTestUtils.setField(comment, "deleted", true);

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.updateComment(userId, commentId, request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.BAD_REQUEST_COMMENT.getMessage());
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    public void deleteComment_success() {
        // given
        String userId = testUserId.toString();
        User user = createTestUser();
        Post post = createTestPost(user);
        Comment comment = new Comment("테스트 댓글입니다.", user, post);

        given(commentRepository.findById(testCommentId)).willReturn(Optional.of(comment));

        // when
        commentService.deleteComment(userId, testCommentId);

        // then
        verify(commentRepository, times(1)).findById(testCommentId);
        assertThat(comment.getDeleted()).isTrue();
    }
}