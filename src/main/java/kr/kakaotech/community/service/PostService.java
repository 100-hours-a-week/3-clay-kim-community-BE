package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.PostRegisterRequest;
import kr.kakaotech.community.dto.response.PostDetailResponse;
import kr.kakaotech.community.dto.response.PostListResponse;
import kr.kakaotech.community.dto.response.PostSummaryResponse;
import kr.kakaotech.community.entity.*;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.PostRepository;
import kr.kakaotech.community.repository.PostStatusRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
//        PostStatus postStatus = postStatusRepository.findById(savePost.getId()).get();
//        postStatus.updateCount(0, 0, 0);
        System.out.println("=== postId: " + savedPost.getId());  // 여기서 2 찍히는지 확인
        PostStatus status = new PostStatus(savedPost);
        postStatusRepository.save(status);

        return savedPost.getId();
    }

    /**
     * 게시글 수정 버튼 권한 체크
     */
    @Transactional(readOnly = true)
    public boolean checkAuthorization(int postId, String userId) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        return post.getUser().getId().toString().equals(userId);
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
     * size 사용
     */
    @Transactional
    public PostListResponse getPostListBySize(Integer cursor, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<Object[]> resultList;

        if (cursor == null) {
//            posts = postRepository.findTopPost(size);
            resultList = postRepository.findTopPost(pageable);
        } else {
//            posts = postRepository.findPostByCursor(cursor, size);
            resultList = postRepository.findPostByCursor(cursor, pageable);
        }

        boolean hasNext = resultList.size() == size;
//        Integer nextCursor = hasNext ? resultList.get(resultList.size() - 1).getId() : null;

//        List<PostSummaryResponse> postList = resultList.stream()
//                .map(PostSummaryResponse::fromEntity)
//                .toList();

        return new PostListResponse(null, null, hasNext);
    }

    /**
     * 게시글 목록 조회
     */
    @Transactional
    public PostListResponse getPostList(Integer cursor, int size) {
        Pageable pageable = PageRequest.of(0, size);
        List<Object[]> resultList;

        if (cursor == null) {
//            posts = postRepository.findTopPost(size);
            resultList = postRepository.findTopPost(pageable);
        } else {
//            posts = postRepository.findPostByCursor(cursor, size);
            resultList = postRepository.findPostByCursor(cursor, pageable);
        }

        List<PostSummaryResponse> postList = resultList.stream()
                .map(result -> {
                    Post post = (Post) result[0];
                    PostStatus postStatus = (PostStatus) result[1];
                    return PostSummaryResponse.fromEntity(post, postStatus);
                })
                .toList();

        boolean hasNext = resultList.size() == size;
        Integer nextCursor = hasNext ? postList.get(postList.size() - 1).getId() : null;

        return new PostListResponse(postList, nextCursor, hasNext);
    }

    /**
     * 게시글 상세조회
     */
    public PostDetailResponse getPostDetails(int postId) {
        Post post = postRepository.findById(postId).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_POST));

        if (post.getDeleted()) {
            throw new CustomException(ErrorCode.NOT_FOUND_POST);
        }

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
}
