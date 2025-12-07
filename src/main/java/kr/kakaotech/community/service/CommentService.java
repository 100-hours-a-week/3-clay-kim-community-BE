package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.CommentRequest;
import kr.kakaotech.community.dto.response.CommentResponse;
import kr.kakaotech.community.entity.Comment;
import kr.kakaotech.community.entity.Post;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CommentRepository;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostStatusRepository postStatusRepository;

    /**
     * 댓글 등록
     */
    @Transactional
    public void registerComment(String userId, int postId, CommentRequest request) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        Comment comment = new Comment(request.getContent(), user, post);
        commentRepository.save(comment);

        postStatusRepository.incrementCommentCount(postId);
    }

    /**
     * 게시글별 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentList(int postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        Page<Comment> commentPage = commentRepository.findByPost(post, pageable);

        return commentPage.map(CommentResponse::fromEntity);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void updateComment(String userId, int commentId, CommentRequest request) {
        Comment comment = validateComment(userId, commentId);

        comment.update(request.getContent());
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(String userId, int commentId) {
        Comment comment = validateComment(userId, commentId);

        comment.delete();
    }

    /**
     * 검증 중복 로직 메서드 처리
     *
     * @param userId
     * @param commentId
     * @return
     */
    private Comment validateComment(String userId, int commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_COMMENT));

        if (!comment.getUser().getId().toString().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (comment.getDeleted()) {
            throw new CustomException(ErrorCode.BAD_REQUEST_COMMENT);
        }

        return comment;
    }
}

