package kr.kakaotech.community.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.response.UserLoginResponse;

public interface AuthService {
    UserLoginResponse getAuth(UserLoginRequest userLoginRequest, HttpServletResponse response);
    void deleteAuth(HttpServletRequest request, HttpServletResponse response);
    void refreshToken(HttpServletRequest request, HttpServletResponse response);
}
