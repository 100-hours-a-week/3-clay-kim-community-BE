package kr.kakaotech.community.auth.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.kakaotech.community.auth.AuthenticationStrategy;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "auth.type", havingValue = "session")
public class SessionFilter implements AuthenticationStrategy {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String Session_Cookie_Name = "JSESSIONID";
    private final String SESSION_PREFIX = "sessionKey:";

    @Value("${session.sessionTtl}")
    private int sessionTtl;

    /**
     * 인증 추출만 진행
     *
     * session은 쿠키에서 추출
     */
    @Override
    public Optional<String> extractAuth(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> Session_Cookie_Name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    /**
     * 인증을 검증하고 request에 넣어줌
     *
     * session은 db에서 검증.
     */
    @Override
    public void setAttributeByAuth(String sessionId, HttpServletRequest request) {
        String key = SESSION_PREFIX + sessionId;

        Object value = redisTemplate.opsForValue().get(key);
        SessionDao sessionDao = null;
        if (value instanceof SessionDao) {
            sessionDao = (SessionDao) value;
        } else if (value instanceof LinkedHashMap) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            sessionDao = mapper.convertValue(value, SessionDao.class);
        }

        if (sessionDao == null) {
            throw new CustomException(ErrorCode.INVALID_SESSION);
        }

        sessionDao.updateLastAccessedTime();
        redisTemplate.opsForValue().set(key, sessionDao, sessionTtl, TimeUnit.SECONDS);

        request.setAttribute("userId", sessionDao.getUserId());
        request.setAttribute("role", sessionDao.getRole());
    }

    public void login(HttpServletRequest request) {
//        try {
//            // getInputStream()으로 body의 json 읽기
//            UserLoginRequest userLoginRequest = new ObjectMapper().readValue(request.getInputStream(), UserLoginRequest.class);
//
//            User user = userRepository.findByEmail(userLoginRequest.getEmail()).orElseThrow(() ->
//                    new CustomException(ErrorCode.NOT_FOUND_USER));
//
//            if (!checkPassword(userLoginRequest.getPassword(), user)) {
//                throw new CustomException(ErrorCode.BAD_PASSWORD);
//            }
//        } catch (IOException e) {
//            throw new CustomException(ErrorCode.NOT_FOUND_USER);
//        }
    }

}
