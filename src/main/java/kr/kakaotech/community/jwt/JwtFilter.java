package kr.kakaotech.community.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // 필터 제외 경로 목록
    private static final String[] EXCLUDED_PATHS = {
            "/auth", "/error", "/posts"
    };

    // 필터 제외 경로 설정
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return Arrays.stream(EXCLUDED_PATHS).anyMatch(path::startsWith);
    }

    /**
     * JWT 추출 및 검증
     *
     * JWT 추출(헤더, 쿠키)
     *
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 현재 요청 경로 추출
        String requestURI = request.getRequestURI();
        log.info("[JwtFilter] 요청 URI: {}", requestURI);

        Optional<String> token = extractToken(request, "accessToken");

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 토큰 검증 및 설정
        validatedAndSetAttribute(token.get(), request);

        filterChain.doFilter(request, response);
    }

    // index 요청인지 확인
    private boolean isIndexRequest (HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/".equals(uri) || "/index".equals(uri);
    }

    // refresh 요청인지 확인
    private boolean isRefresh (HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/refresh".equals(uri);
    }

    // 토큰 추출
    private Optional<String> extractToken(HttpServletRequest request, String tokenName) {
        return extractTokenFromCookie(request, tokenName)
                .or(() -> extractTokenFromHeader(request));
    }

    // 헤더 토큰 추출
    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }

    // 쿠키 토큰 추출
    private Optional<String> extractTokenFromCookie(HttpServletRequest request, String tokenName) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> tokenName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void validatedAndSetAttribute(String token, HttpServletRequest request) {
        try {
            // claims 얻으면서 만료 검증도 같이 진행됨
            Claims claims = jwtProvider.parseToken(token);

            request.setAttribute("userId", claims.getSubject());
            request.setAttribute("role", claims.get("role", String.class));

            log.info("Request URI : {}", request.getRequestURI());
        } catch (SignatureException e) {
            log.error("JWT 서명 에러");
            throw new CustomException(ErrorCode.NON_SIGNATURE_JWT);
        } catch (ExpiredJwtException e) {
            log.error("JWT 기간 만료");
            throw new CustomException(ErrorCode.EXPIRED_JWT);
        } catch (Exception e) {
            log.error("[JwtFilter 에러] : {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }
}
