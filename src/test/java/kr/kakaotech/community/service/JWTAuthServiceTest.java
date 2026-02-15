package kr.kakaotech.community.service;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.auth.jwt.JwtProvider;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.response.UserLoginResponse;
import kr.kakaotech.community.entity.RefreshToken;
import kr.kakaotech.community.entity.RefreshTokenRepository;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.entity.UserRole;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTAuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    JWTAuthService jwtAuthService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("encodedPassword")
                .nickname("테스터")
                .role(UserRole.USER)
                .build();

        ReflectionTestUtils.setField(jwtAuthService, "accessTtl", 1800);
        ReflectionTestUtils.setField(jwtAuthService, "refreshTtl", 604800);
    }

    @Nested
    @DisplayName("로그인 (getAuth)")
    class GetAuth {

        @Test
        @DisplayName("성공 - 토큰 발급 및 쿠키 설정")
        void success() {
            // given
            UserLoginRequest request = new UserLoginRequest("test@email.com", "password");
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
            given(jwtProvider.createAccess(userId.toString(), "USER")).willReturn("access-token");
            given(jwtProvider.createRefresh(userId.toString(), "USER")).willReturn("refresh-token");
            given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(null);

            // when
            UserLoginResponse loginResponse = jwtAuthService.getAuth(request, response);

            // then
            assertThat(loginResponse.getNickname()).isEqualTo("테스터");
            assertThat(loginResponse.getUserEmail()).isEqualTo("test@email.com");
            assertThat(loginResponse.getUserId()).isEqualTo(userId.toString());

            verify(refreshTokenRepository).deleteByUserId(userId);
            verify(jwtProvider).createAccess(userId.toString(), "USER");
            verify(jwtProvider).createRefresh(userId.toString(), "USER");
            verify(refreshTokenRepository).save(any(RefreshToken.class));

            // 쿠키 설정 확인
            Cookie accessCookie = response.getCookie("accessToken");
            Cookie refreshCookie = response.getCookie("refreshToken");
            assertThat(accessCookie).isNotNull();
            assertThat(accessCookie.getValue()).isEqualTo("access-token");
            assertThat(accessCookie.isHttpOnly()).isTrue();
            assertThat(refreshCookie).isNotNull();
            assertThat(refreshCookie.getValue()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 이메일")
        void fail_emailNotFound() {
            // given
            UserLoginRequest request = new UserLoginRequest("wrong@email.com", "password");
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(userRepository.findByEmail("wrong@email.com")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jwtAuthService.getAuth(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));

            verify(jwtProvider, never()).createAccess(any(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void fail_wrongPassword() {
            // given
            UserLoginRequest request = new UserLoginRequest("test@email.com", "wrongPassword");
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> jwtAuthService.getAuth(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_PASSWORD));

            verify(refreshTokenRepository, never()).deleteByUserId(any());
            verify(jwtProvider, never()).createAccess(any(), any());
        }
    }

    private Claims createClaims(String subject) {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn(subject);
        return claims;
    }

    @Nested
    @DisplayName("로그아웃 (deleteAuth)")
    class DeleteAuth {

        @Test
        @DisplayName("성공 - 쿠키 삭제 및 DB 토큰 삭제")
        void success() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("refreshToken", "valid-refresh-token"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            Claims claims = createClaims(userId.toString());

            given(jwtProvider.parseToken("valid-refresh-token")).willReturn(claims);

            // when
            jwtAuthService.deleteAuth(request, response);

            // then
            verify(refreshTokenRepository).deleteByUserId(userId);

            Cookie accessCookie = response.getCookie("accessToken");
            Cookie refreshCookie = response.getCookie("refreshToken");
            assertThat(accessCookie).isNotNull();
            assertThat(accessCookie.getMaxAge()).isEqualTo(0);
            assertThat(refreshCookie).isNotNull();
            assertThat(refreshCookie.getMaxAge()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패 - 리프레시 토큰 쿠키 없음")
        void fail_noRefreshTokenCookie() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            // 쿠키 없음
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when & then
            assertThatThrownBy(() -> jwtAuthService.deleteAuth(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }

    @Nested
    @DisplayName("토큰 갱신 (refreshToken)")
    class RefreshTokenTest {

        @Test
        @DisplayName("성공 - 새 토큰 발급")
        void success() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("refreshToken", "old-refresh-token"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            Claims claims = createClaims(userId.toString());

            RefreshToken storedToken = new RefreshToken(userId, "old-refresh-token", 604800);

            given(jwtProvider.parseToken("old-refresh-token")).willReturn(claims);
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(storedToken));
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(jwtProvider.createAccess(userId.toString(), "USER")).willReturn("new-access-token");
            given(jwtProvider.createRefresh(userId.toString(), "USER")).willReturn("new-refresh-token");
            given(refreshTokenRepository.save(any(RefreshToken.class))).willReturn(null);

            // when
            jwtAuthService.refreshToken(request, response);

            // then
            verify(refreshTokenRepository).deleteByUserId(userId);
            verify(jwtProvider).createAccess(userId.toString(), "USER");
            verify(jwtProvider).createRefresh(userId.toString(), "USER");
            verify(refreshTokenRepository).save(any(RefreshToken.class));

            Cookie accessCookie = response.getCookie("accessToken");
            assertThat(accessCookie).isNotNull();
            assertThat(accessCookie.getValue()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("실패 - DB에 리프레시 토큰 없음 (이미 로그아웃)")
        void fail_tokenNotInDB() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("refreshToken", "orphan-token"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            Claims claims = createClaims(userId.toString());

            given(jwtProvider.parseToken("orphan-token")).willReturn(claims);
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jwtAuthService.refreshToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("실패 - DB 토큰과 쿠키 토큰 불일치 (토큰 재사용 감지)")
        void fail_tokenMismatch() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("refreshToken", "stolen-token"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            Claims claims = createClaims(userId.toString());

            RefreshToken storedToken = new RefreshToken(userId, "real-token", 604800);

            given(jwtProvider.parseToken("stolen-token")).willReturn(claims);
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(storedToken));

            // when & then
            assertThatThrownBy(() -> jwtAuthService.refreshToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("실패 - 리프레시 토큰 쿠키 없음")
        void fail_noCookie() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when & then
            assertThatThrownBy(() -> jwtAuthService.refreshToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }
}
