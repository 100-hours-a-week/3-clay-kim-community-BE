package kr.kakaotech.community.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface AuthenticationStrategy {
    // 자격증명을 위한 token 또는 sessionID 추출
    Optional<String> extractAuth(HttpServletRequest request);
    // 자격증명 검증 & DB 조회를 통해 얻은 값을 주입
    void setAttributeByAuth(String auth, HttpServletRequest request);
}
