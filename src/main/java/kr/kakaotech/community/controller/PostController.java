package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.request.PostRegisterRequest;
import kr.kakaotech.community.dto.response.PostDetailResponse;
import kr.kakaotech.community.dto.response.PostListResponse;
import kr.kakaotech.community.dto.response.PostStatusResponse;
import kr.kakaotech.community.global.FakeUserProvider;
import kr.kakaotech.community.global.security.CustomUserDetails;
import kr.kakaotech.community.service.PostService;
import kr.kakaotech.community.service.PostStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
public class PostController {

    private final FakeUserProvider fakeUserProvider;
    private final PostService postService;
    private final PostStatusService postStatusService;

    /**
     * 게시글 작성
     */
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Integer>> registerPost(@RequestBody PostRegisterRequest postRegisterRequest, HttpServletRequest httpServletRequest) {
        return ApiResponse.create("게시글 등록 성공", postService.registerPost(httpServletRequest.getAttribute("userId").toString(), postRegisterRequest));
    }

    /**
     * 게시글 목록 불러오기
     */
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getPostList(
            @RequestParam(required = false) Integer cursor,
            @RequestParam(defaultValue = "5") int size) {

        PostListResponse response = postService.getPostList(cursor, size);

        ApiResponse<PostListResponse> apiResponse = new ApiResponse<>("게시글 목록 조회 성공", response);
        return ResponseEntity.ok(apiResponse);
    }


    /**
     * 게시글 상세 조회
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse> getPost(@PathVariable int postId) {
        PostDetailResponse response = postService.getPostDetails(postId);

        postStatusService.incrementViewCount(postId);

        ApiResponse<PostDetailResponse> apiResponse = new ApiResponse("게시글 상세 내용입니다.", response);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 게시글의 Status 가져오기
     */
    @GetMapping("/posts/{postId}/statuses")
    public ResponseEntity<ApiResponse<PostStatusResponse>> getPostStatus(@PathVariable int postId) {
        PostStatusResponse postStatusResponse = postStatusService.getPostStatus(postId);

        return ApiResponse.success("게시글 Status 입니다.", postStatusResponse);
    }

    /**
     * 게시글 권한 체크
     */
    @GetMapping("/posts/{postId}/auth")
    public ResponseEntity<ApiResponse<Boolean>> checkPostAuthorization(@PathVariable int postId) {
        String fakeUserId = fakeUserProvider.getCurrentUserId().toString();

        boolean isAuth = postService.checkAuthorization(postId, fakeUserId);
        ApiResponse<Boolean> apiResponse = new ApiResponse<>("권한 체크 성공.", isAuth);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 게시글 수정
     */
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Object>> registerPost(@PathVariable int postId, HttpServletRequest httpServletRequest, @RequestBody PostRegisterRequest postRegisterRequest) {

        String userId = httpServletRequest.getAttribute("userId").toString();

        postService.updatePost(postId, userId, postRegisterRequest);

        return ApiResponse.success("게시글 수정 성공", null);
    }

    /**
     * 게시글 삭제
     */
    @PatchMapping("/posts/{postId}/deactivation")
    public ResponseEntity<ApiResponse<Object>> deactivatePost(@PathVariable int postId, HttpServletRequest httpServletRequest) {
        postService.deletePost(postId, httpServletRequest.getAttribute("userId").toString());

        return ApiResponse.success("삭제 성공", null);
    }

    /**
     * 게시글 상태 맞추기
     */
    @PostMapping("/post-status")
    public ResponseEntity<ApiResponse<Object>> syncPostStatus() {
        postStatusService.syncToDatabase();

        return ApiResponse.success("싱크 성공", null);
    }
}
