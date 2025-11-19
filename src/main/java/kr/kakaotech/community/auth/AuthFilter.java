package kr.kakaotech.community.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.auth.jwt.JwtFilter;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthFilter extends OncePerRequestFilter {

    // 필터 제외 경로 목록
    @Value("${exclusive-path}")
    private List<String> EXCLUDED_PATHS;

    private final AuthenticationStrategy authStrategy;
    private final ObjectMapper objectMapper;

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 인증 불필요 경로
        if (EXCLUDED_PATHS.stream().anyMatch(uri::startsWith)) {
            return true;
        }

        // GET 요청 중 인증 불필요한 것들
        if ("GET".equals(method)) {
            return uri.matches("/api/users/email") ||
                   uri.matches("/api/users/nickname") ||
                   uri.matches("/api/posts") ||
                   uri.matches("/api/posts/\\d+") ||
                   uri.matches("/api/posts/\\d+/comments");
        }

        if ("POST".equals(method)) {
            return uri.matches("/api/users");
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 현재 요청 경로 추출
        String requestURI = request.getRequestURI();
        log.info("[JwtFilter] 요청 URI: {}", requestURI);

        // jwt or Session extract
        Optional<String> auth = authStrategy.extractAuth(request);

        if (auth.isEmpty()) {
            log.info("is isJWTStrategy ? {}", isJWTStrategy());
            log.info("is hasRefreshToken ? {}", hasRefreshToken(request));

            if (isJWTStrategy() && hasRefreshToken(request)) {
                authExceptionHandler(response, new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN));
                return;
            } else {
                if (requestURI.matches("/posts/\\d+/likes")) {
                    filterChain.doFilter(request, response);
                    return;
                }
                authExceptionHandler(response, new CustomException(ErrorCode.INVALID_TOKEN));
                return;
            }
        }

        // jwt or session
        authStrategy.setAttributeByAuth(auth.get(), request);

        filterChain.doFilter(request, response);
    }

    private boolean isJWTStrategy() {
        return authStrategy instanceof JwtFilter;
    }

    private boolean hasRefreshToken(HttpServletRequest request) {
        if (authStrategy instanceof JwtFilter) {
            return ((JwtFilter) authStrategy).hasRefreshToken(request);
        }
        return false;
    }

    private void authExceptionHandler(HttpServletResponse response, CustomException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(exception));
    }
}
