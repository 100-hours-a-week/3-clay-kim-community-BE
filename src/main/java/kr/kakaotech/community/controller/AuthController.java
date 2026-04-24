package kr.kakaotech.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.response.UserLoginResponse;
import kr.kakaotech.community.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 API")
@Slf4j
@RequiredArgsConstructor
@Controller
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다. JWT 토큰이 쿠키에 설정됩니다.")
    @PostMapping("/auth")
    public ResponseEntity<ApiResponse<UserLoginResponse>> getTokenByLogin(@Valid @RequestBody UserLoginRequest userLoginRequest, HttpServletResponse response) {
        UserLoginResponse userLoginResponse = authService.getAuth(userLoginRequest, response);
        log.info(response.toString());
        return ApiResponse.success("로그인 성공", userLoginResponse);
    }

    @Operation(summary = "로그아웃", description = "쿠키 및 DB에서 토큰을 삭제합니다.")
    @PostMapping("/auth/token")
    public ResponseEntity<ApiResponse<Object>> getToken(HttpServletRequest request, HttpServletResponse response) {
        authService.deleteAuth(request, response);
        return ApiResponse.success("로그아웃 성공", null);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급합니다.")
    @GetMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        authService.refreshToken(request, response);
        return ApiResponse.success("success", true);
    }
}
