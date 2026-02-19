package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.RefreshTokenRepository;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.type", havingValue = "jwt")
public class TestUserAuthService {

    private static final String TEST_USER_EMAIL = "testuser";
    private static final String TEST_USER_PASSWORD = "testuser";
    private static final String TEST_USER_NICKNAME = "testuser";
    private static final String TEST_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTAuthService jwtAuthService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String issueAccessTokenForTestUser() {
        User user = userRepository.findByEmail(TEST_USER_EMAIL)
                .orElseGet(this::createTestUser);

        refreshTokenRepository.deleteByUserId(user.getId());
        JWTAuthService.TokenResponse tokenResponse = jwtAuthService.generateAndSaveToken(user);

        return tokenResponse.accessToken();
    }

    private User createTestUser() {
        User user = new User(
                TEST_USER_EMAIL,
                passwordEncoder.encode(TEST_USER_PASSWORD),
                TEST_USER_NICKNAME,
                TEST_USER_ROLE
        );

        return userRepository.save(user);
    }
}
