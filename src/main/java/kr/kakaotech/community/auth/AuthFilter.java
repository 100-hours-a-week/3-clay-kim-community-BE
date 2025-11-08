package kr.kakaotech.community.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
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

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String method = request.getMethod();
        if ("POST".equals(method)) {
            return false;
        }
        if ("OPTIONS".equals(method)) {
            return true;
        }

        String path = request.getRequestURI();

        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 현재 요청 경로 추출
        String requestURI = request.getRequestURI();
        log.info("[JwtFilter] 요청 URI: {}", requestURI);

        // jwt or Session extract
        Optional<String> auth = authStrategy.extractAuth(request);

        if (!auth.isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }

        // jwt or session
        authStrategy.setAttributeByAuth(auth.get(), request);

        filterChain.doFilter(request, response);
    }
}
