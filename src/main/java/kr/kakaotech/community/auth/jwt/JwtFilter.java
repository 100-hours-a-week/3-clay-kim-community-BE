package kr.kakaotech.community.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.auth.AuthenticationStrategy;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "auth.type", havingValue = "jwt")
public class JwtFilter implements AuthenticationStrategy {

    private final JwtProvider jwtProvider;
    private final String ACCESS_TOKEN = "accessToken";
    private final String REFRESH_TOKEN = "refreshToken";

    /**
     * JWT 추출 및 검증
     *
     * JWT 추출(헤더, 쿠키)
     *
     */
    @Override
    public Optional<String> extractAuth(HttpServletRequest request) {
        return extractTokenFromCookie(request, ACCESS_TOKEN)
                .or(() -> extractTokenFromHeader(request));
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

    // 헤더 토큰 추출
    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }

    @Override
    public void setAttributeByAuth(String token, HttpServletRequest request) {
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
            log.info("JWT 기간 만료");
            throw new CustomException(ErrorCode.EXPIRED_JWT);
        } catch (Exception e) {
            log.error("[JwtFilter 에러] : {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    public boolean hasRefreshToken(HttpServletRequest request) {
        return extractTokenFromCookie(request, REFRESH_TOKEN).isPresent();
    }
}
