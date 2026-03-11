package kr.kakaotech.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.response.TestUserTokenResponse;
import kr.kakaotech.community.service.TestUserAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.type", havingValue = "jwt")
public class TestAuthController {

    private final TestUserAuthService testUserAuthService;

    @Operation(summary = "테스트 유저 토큰 발급", description = "testuser 계정이 없으면 생성하고, 있으면 기존 계정으로 Access Token을 발급합니다.")
    @PostMapping("/auth/testuser")
    public ResponseEntity<ApiResponse<TestUserTokenResponse>> issueTestUserAccessToken() {
        String accessToken = testUserAuthService.issueAccessTokenForTestUser();
        return ApiResponse.success("테스트 유저 access token 발급 성공", new TestUserTokenResponse(accessToken));
    }
}
