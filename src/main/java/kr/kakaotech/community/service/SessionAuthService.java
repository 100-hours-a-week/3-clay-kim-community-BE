package kr.kakaotech.community.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.auth.session.SessionDao;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.response.UserLoginResponse;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.type", havingValue = "session")
@Service
public class SessionAuthService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${session.sessionTtl}")
    private int sessionTtl;
    private final String Session_Cookie_Name = "JSESSIONID";
    private final String SESSION_PREFIX = "sessionKey:";

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

        // 세션 생성
        String sessionId = UUID.randomUUID().toString();
        SessionDao sessionDao = new SessionDao(
                sessionId,
                user.getId().toString(),
                user.getRole().toString(),
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(sessionTtl)
        );

        // 세션 저장
        addTokenCookie(response, Session_Cookie_Name, sessionId, sessionTtl);
        redisTemplate.opsForValue().set(SESSION_PREFIX + sessionId, sessionDao, sessionTtl, TimeUnit.SECONDS);


        return new UserLoginResponse(user.getNickname(), user.getEmail(), user.getId().toString());
    }

    @Override
    public void deleteAuth(HttpServletRequest request, HttpServletResponse response) {
        addTokenCookie(response, Session_Cookie_Name, null, 0);

        String sessionId = extractedSessionId(request);

        redisTemplate.delete(SESSION_PREFIX + sessionId);
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
    }

    private boolean checkPassword(String password, User user) {
        return passwordEncoder.matches(password, user.getPassword());
    }

    public void addTokenCookie(HttpServletResponse response, String cookieName, String cookieValue, int maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);

        response.addCookie(cookie);
    }

    private String extractedSessionId(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies()).stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> Session_Cookie_Name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst().orElseThrow(() ->
                        new CustomException(ErrorCode.INVALID_SESSION));
    }
}
