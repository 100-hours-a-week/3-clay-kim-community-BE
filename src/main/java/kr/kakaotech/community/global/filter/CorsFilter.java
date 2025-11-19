package kr.kakaotech.community.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.global.config.CorsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class CorsFilter extends OncePerRequestFilter {

    private final CorsProperties corsProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");
        log.debug("[CorsFilter] Origin: {}, Method: {}, URI: {}", origin, request.getMethod(), request.getRequestURI());

        // origin이 허용 목록에 있으면 해당 origin을 설정
        if (origin != null && corsProperties.getAllowedOrigins().contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers",
                "Content-Type, Authorization, access, refresh, X-Requested-With");
            response.setHeader("Access-Control-Expose-Headers",
                String.join(", ", corsProperties.getExposedHeaders()));
            response.setHeader("Access-Control-Max-Age", "3600");

            log.debug("[CorsFilter] CORS headers added for origin: {}", origin);
        } else {
            log.warn("[CorsFilter] Origin not allowed or missing: {}", origin);
        }

        // OPTIONS 요청은 여기서 바로 응답
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            log.debug("[CorsFilter] Preflight request handled");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
