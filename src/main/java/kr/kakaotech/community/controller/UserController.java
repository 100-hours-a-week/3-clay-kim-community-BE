package kr.kakaotech.community.controller;

import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.request.UserPasswordRequest;
import kr.kakaotech.community.dto.request.UserRegisterRequest;
import kr.kakaotech.community.dto.request.UserUpdateRequest;
import kr.kakaotech.community.dto.response.UserDetailResponse;
import kr.kakaotech.community.global.security.CustomUserDetails;
import kr.kakaotech.community.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping(value = "/users")
    public ResponseEntity<ApiResponse<String>> register(@ModelAttribute UserRegisterRequest userDto,
                                                        @RequestPart(value = "profileImage", required = false) MultipartFile image) {
        userService.registerUser(userDto, image);

        return ApiResponse.create("회원가입 성공", userDto.getEmail());
    }

    /**
     * 특정 회원 불러오기
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUser(@PathVariable String userId) {
        UserDetailResponse userDetailResponse = userService.getUser(userId);

        return ApiResponse.success("단일 회원 조회 성공", userDetailResponse);
    }

    /**
     * 회원 리스트 불러오기
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> getUserList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<UserDetailResponse> userPage = userService.getUserPage(pageable);

        ApiResponse<Page<UserDetailResponse>> response = new ApiResponse<>("모든 회원 불러오기 성공", userPage);
        return ResponseEntity.status(200).body(response);
    }

    /**
     * 회원 정보 업데이트
     */
    @PatchMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest userUpdateRequest) {
        UserDetailResponse userDetailResponse = userService.updateUser(userId, userUpdateRequest);

        ApiResponse<UserDetailResponse> apiResponse = new ApiResponse<>("업데이트 성공", userDetailResponse);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * 회원 탈퇴 - soft delete
     */
    @PatchMapping("/users/{userId}/deactivation")
    public void deleteUser(@PathVariable String userId) {
        userService.softDeleteUser(userId);
    }

    /**
     * 유저 input 실시간 검증
     *
     * 이메일과 닉네임을 실시간으로 검증하여 결과를 반환합니다.
     */
    @GetMapping("/users/email")
    public ResponseEntity<ApiResponse<Boolean>> checkUserEmail(@RequestParam String email, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String userInfo = uri.substring(uri.lastIndexOf('/') + 1);

        return ApiResponse.success("duplication 결과", userService.duplicateCheckUserInfo(userInfo, email));
    }

    /**
     * 닉네임 실시간 중복 검증
     */
    @GetMapping("/users/nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkUserNickname(@RequestParam String nickname, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String userInfo = uri.substring(uri.lastIndexOf('/') + 1);

        return ApiResponse.success("duplication 결과", userService.duplicateCheckUserInfo(userInfo, nickname));
    }

    /**
     * 비밀번호 변경
     */
    @PatchMapping("/users/password")
    public ResponseEntity<ApiResponse<Boolean>> changePassword(@RequestBody UserPasswordRequest userPasswordRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success("비밀번호 수정 결과", userService.changePassword(userDetails.getUserId(), userPasswordRequest));
    }
}
