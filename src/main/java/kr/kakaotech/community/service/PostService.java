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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final ImageService imageService;

    private final int IMAGE_LIMIT_COUNT = 5;
    private final PostStatusService postStatusService;

    /**
     * Post 등록
     */
    @Transactional
    public int registerPost(String userId, PostRegisterRequest request, List<MultipartFile> images) {
        if (images != null && images.size() > IMAGE_LIMIT_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_TOO_MANY);
        }

        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(PostType.valueOf(request.getType().toUpperCase()))
                .nickname(getUser.getNickname())
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .user(getUser)
                .build();

        // 이미지 저장
        if (images != null && !images.isEmpty()) {
            post.saveImage(imageService.saveImage(images, post));
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
    public void updatePost(int postId, String userId, PostModifyRequest request, List<MultipartFile> images) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        if (!post.getUser().getId().toString().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (post.getPostImages() != null && images != null && post.getPostImages().size() + images.size() > IMAGE_LIMIT_COUNT) {
            throw new CustomException(ErrorCode.IMAGE_TOO_MANY);
        }

        // 기본 정보 수정
        post.updatePost(request);

        // 이미지 삭제: removeImageIds에 있는 이미지들을 PostImage 리스트에서 제거
        if (request.getRemoveImageIds() != null && !request.getRemoveImageIds().isEmpty()) {
            post.getPostImages().removeIf(postImage ->
                    request.getRemoveImageIds().contains(postImage.getImage().getId())
            );
        }

        // 새 이미지 추가
        if (images != null && !images.isEmpty()) {
            List<PostImage> newPostImages = imageService.saveImage(images, post);
            post.getPostImages().addAll(newPostImages);
        }
    }

    /**
     * 인덱스용 이미지 포함 게시글 목록 조회
     */
    @Transactional
    public List<PostSummaryWithImageResponse> getPostListWithImage(int size) {
        Pageable pageable = PageRequest.of(0, size);

        List<PostSummaryWithImageResponse> postWithImage = postRepository.findPostWithImage(pageable);

        return postWithImage;
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
     * nickname에 따른 검색
     */
    public PostListResponse getNicknamePostList(Integer cursor, String nickname, int size) {
        List<PostSummaryResponse> postList = postRepository.findPostByNickname(
                nickname,
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
        Post post = postRepository.findPostDetailsWithImages(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POST));

        // Post의 PostImage들에서 ImageResponse로 변환 (imageId + url)
        List<ImageResponse> images = post.getPostImages().stream()
                .map(postImage -> new ImageResponse(
                        postImage.getImage().getId(),
                        postImage.getImage().getUrl()
                ))
                .toList();

        return new PostDetailResponse(
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUser().getId(),
                post.getNickname(),
                post.getUser().getImage().getUrl(),
                post.getType(),
                images
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
