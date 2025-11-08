package kr.kakaotech.community.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.response.UserLoginResponse;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.auth.jwt.JwtProvider;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.type", havingValue = "jwt")
@Service
public class JWTAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.expirationtime.accessTtl}")
    private int accessTtl;
    @Value("${jwt.expirationtime.refreshTtl}")
    private int refreshTtl;

    private final String ACCESS_TOKEN = "accessToken";
    private final String REFRESH_TOKEN = "refreshToken";
    private final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Transactional
    @Override
    public UserLoginResponse getAuth(UserLoginRequest request, HttpServletResponse response) {
        String email = request.getEmail();
        String password = request.getPassword();

        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        if (!checkPassword(password, user)) {
            throw new CustomException(ErrorCode.BAD_PASSWORD);
        }

        // 기존 리프레시 토큰 무효화
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + user.getId().toString());

        // 토큰 발급 및 저장
        TokenResponse tokenResponse = generateAndSaveToken(user);
        setCookie(response, tokenResponse);

        return new UserLoginResponse(user.getNickname(), user.getEmail(), user.getId().toString());
    }

    /**
     * 로그아웃
     *
     * 쿠키 maxAge 0으로 설정
     * redis 삭제
     */
    @Override
    public void deleteAuth(HttpServletRequest request, HttpServletResponse response) {
        addTokenCookie(response, ACCESS_TOKEN, null, 0);
        addTokenCookie(response, REFRESH_TOKEN, null, 0);

        String refreshToken = extractedRefreshToken(request);

        // RefreshToken 검증 및 만료확인
        Claims refreshClaims = jwtProvider.parseToken(refreshToken);
        String userId = refreshClaims.getSubject();

        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }

    /**
     * ACCESS_TOKEN 갱신
     *
     * refreshToken 만료확인
     * redis에 있는지 확인
     * ACCESS_TOKEN & refreshToken 발급
     * Cookie 생성
     */
    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키 확인
        String refreshToken = extractedRefreshToken(request);

        // RefreshToken 검증 및 만료확인
        Claims refreshClaims = jwtProvider.parseToken(refreshToken);
        String userId = refreshClaims.getSubject();

        // redis 검증
        String refreshTokenFromRedis = getRefreshTokenFromRedis(userId);
        if (refreshTokenFromRedis == null || !refreshTokenFromRedis.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 쿠키 재발급(Access + Refresh, Refresh Redis 등록)
        User user = userRepository.findById(UUID.fromString(userId)).get();
        setCookie(response, generateAndSaveToken(user));
    }

    private String extractedRefreshToken(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> REFRESH_TOKEN.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst().orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_TOKEN));
    }

    private String getRefreshTokenFromRedis(String userId) {
        return (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    private void setCookie(HttpServletResponse response, TokenResponse tokenResponse) {
        addTokenCookie(response, ACCESS_TOKEN, tokenResponse.accessToken, accessTtl);
        addTokenCookie(response, REFRESH_TOKEN, tokenResponse.refreshToken, refreshTtl);
    }

    private boolean checkPassword(String password, User user) {
        return passwordEncoder.matches(password, user.getPassword());
    }

    public TokenResponse generateAndSaveToken(User user) {
        String accessToken = jwtProvider.createAccess(user.getId().toString(), user.getRole().toString());
        String refreshToken = jwtProvider.createRefresh(user.getId().toString(), user.getRole().toString());

        // Refresh 토큰만 Redis 저장
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + user.getId().toString(), refreshToken, refreshTtl, TimeUnit.SECONDS);

        return new TokenResponse(accessToken, refreshToken);
    }

    public void addTokenCookie(HttpServletResponse response, String cookieName, String cookieValue, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);

        response.addCookie(cookie);
    }

    public record TokenResponse(String accessToken, String refreshToken) {}
}
