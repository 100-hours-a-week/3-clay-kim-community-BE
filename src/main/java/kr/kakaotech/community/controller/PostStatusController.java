package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.response.LikeResponse;
import kr.kakaotech.community.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
public class PostStatusController {
    private final LikeService likeService;

    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleLike(@PathVariable int postId, HttpServletRequest request) {
        UUID userId = UUID.fromString(request.getAttribute("userId").toString());

        return ApiResponse.success("좋아요 토글 성공", likeService.toggleLike(userId, postId));
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> getLikeStatus(@PathVariable int postId, HttpServletRequest request) {
        Optional<Object> optionalUserId = Optional.ofNullable(request.getAttribute("userId"));

        LikeResponse likeResponse = new LikeResponse(likeService.getLikeStatus(optionalUserId, postId), likeService.getLikeCount(postId));
        return ApiResponse.success("좋아요 상태", likeResponse);
    }

}
