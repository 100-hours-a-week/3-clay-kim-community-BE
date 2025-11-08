package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.request.CommentRequest;
import kr.kakaotech.community.dto.response.CommentResponse;
import kr.kakaotech.community.service.CommentService;
import kr.kakaotech.community.service.PostStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;
    private final PostStatusService postStatusService;

    /**
     * 댓글 등록
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> registerComment(@PathVariable int postId, @RequestBody CommentRequest request, HttpServletRequest httpServletRequest) {
        commentService.registerComment(httpServletRequest.getAttribute("userId").toString(), postId, request);
        postStatusService.incrementCommentCount(postId);

        ApiResponse<Void> apiResponse = new ApiResponse<>("댓글 등록 성공", null);
        return ResponseEntity.status(201).body(apiResponse);
    }

    /**
     * 게시글별 댓글 목록 조회 (페이징)
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getCommentList(
            @PathVariable int postId,
            @PageableDefault(size = 100, sort = "createdAt") Pageable pageable
    ) {
        Page<CommentResponse> response = commentService.getCommentList(postId, pageable);

        ApiResponse<Page<CommentResponse>> apiResponse = new ApiResponse<>("댓글 목록 조회 성공", response);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 댓글 수정
     */
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(@PathVariable int commentId, @RequestBody CommentRequest request, HttpServletRequest httpServletRequest) {
        commentService.updateComment(httpServletRequest.getAttribute("userId").toString(), commentId, request);

        ApiResponse<Void> apiResponse = new ApiResponse<>("댓글 수정 성공", null);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 댓글 삭제
     */
    @PatchMapping("/comments/{commentId}/deactivation")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable int commentId, HttpServletRequest httpServletRequest) {
        commentService.deleteComment(httpServletRequest.getAttribute("userId").toString(), commentId);

        ApiResponse<Void> apiResponse = new ApiResponse<>("댓글 삭제 성공", null);
        return ResponseEntity.ok(apiResponse);
    }
}


