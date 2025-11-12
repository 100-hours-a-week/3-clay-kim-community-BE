package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.PostRegisterRequest;
import kr.kakaotech.community.dto.response.*;
import kr.kakaotech.community.entity.*;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostStatusRepository postStatusRepository;

    /**
     * Post 등록
     */
    @Transactional
    public int registerPost(String userId, PostRegisterRequest request) {
        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        Post post = Post.toEntity(request, getUser);

        // 이미지 저장
        if (request.getUrlList() != null) {
            for (String url : request.getUrlList()) {
                Image image = new Image(url);
                PostImage postImage = new PostImage(image);
                post.saveImage(postImage);
            }
        }

        Post savedPost = postRepository.saveAndFlush(post);
        log.info("=== postId: " + savedPost.getId());
        PostStatus status = new PostStatus(savedPost);
        postStatusRepository.save(status);

        return savedPost.getId();
    }

    /**
     * 게시글 내용 수정
     */
    @Transactional
    public void updatePost(int postId, String userId, PostRegisterRequest request) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        if (!post.getUser().getId().toString().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        post.updatePost(request);
    }

    /**
     * 게시글 목록 조회
     */
    @Transactional
    public PostListResponse getPostList(Integer cursor, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<PostSummaryResponse> postList;

        if (cursor == null) {
            postList = postRepository.findTopPost(pageable);
        } else {
            postList = postRepository.findPostByCursor(cursor, pageable);
        }

        return getPostListAndNextCursorResponse(size, postList);
    }

    /**
     * 기간에 따른 인기글 목록 메서드
     */
    public PostListResponse getLikePostList(Integer cursor, String period, int size) {
        LocalDateTime startDate = switch (period) {
            case "daily" -> LocalDateTime.now().minusDays(1);
            case "weekly" -> LocalDateTime.now().minusDays(7);
            default -> throw new CustomException(ErrorCode.BAD_REQUEST_FILTER);
        };

        List<PostSummaryResponse> postList = postRepository.findPostByLikeCount(
                startDate,
                PageRequest.of(cursor == null ? 0 : cursor, size)
        );

        return getPostListAndNextCursorResponse(size, postList);
    }

    /**
     * TOP 10 좋아요 순서 정렬
     */
    public PostListResponse getPostTop10List() {
        List<PostSummaryResponse> postList = postRepository.findTop10Post(PageRequest.of(0, 10));

        return getPostListAndNextCursorResponse(11, postList);
    }

    /**
     * 게시글 상세조회
     */
    @Transactional
    public PostDetailResponse getPostDetails(int postId) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        if (post.getDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

        postStatusRepository.incrementViewCount(postId);

        return new PostDetailResponse(
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUser().getId().toString(),
                post.getNickname()
        );
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(int postId, String userId) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        if (post.getDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

        post.deletePost();
    }

    /**
     * JPA 결과를 Response로 변환해 줍니다.
     * <p>
     * JPA 결과 - Post, PostStatus
     * List 사이즈를 확인 후 nextCursor와 hasNext 반환
     */
    private PostListResponse getPostListAndNextCursorResponse(int size, List<PostSummaryResponse> postList) {
        boolean hasNext = postList.size() == size;
        Integer nextCursor = hasNext ? postList.get(postList.size() - 1).getId() : null;

        return new PostListResponse(postList, nextCursor, hasNext);
    }
}
