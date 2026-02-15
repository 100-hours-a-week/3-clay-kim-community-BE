package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.UserPasswordRequest;
import kr.kakaotech.community.dto.request.UserRegisterRequest;
import kr.kakaotech.community.dto.request.UserUpdateRequest;
import kr.kakaotech.community.dto.response.UserDetailResponse;
import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.entity.UserRole;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class UserServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    ImageService imageService;
    @Mock
    ImageRepository imageRepository;

    @InjectMocks
    UserService userService;

    private UUID userId;
    private User user;
    private Image defaultImage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        defaultImage = new Image("https://default-image.com/1.png");
        ReflectionTestUtils.setField(defaultImage, "id", 1);

        user = User.builder()
                .id(userId)
                .email("test@email.com")
                .password("encodedPassword")
                .nickname("테스터")
                .deleted(false)
                .role(UserRole.USER)
                .image(defaultImage)
                .build();
    }

    @Nested
    @DisplayName("회원가입 (registerUser)")
    class RegisterUser {

        @Test
        @DisplayName("성공 - 이미지 없이 기본 이미지로 가입")
        void success_withDefaultImage() {
            // given
            UserRegisterRequest request = new UserRegisterRequest("new@email.com", "newUser", "password", "USER");

            given(userRepository.existsByNickname("newUser")).willReturn(false);
            given(userRepository.existsByEmail("new@email.com")).willReturn(false);
            given(passwordEncoder.encode("password")).willReturn("encodedPassword");
            given(imageService.getDefaultImage()).willReturn(defaultImage);

            // when
            userService.registerUser(request, null);

            // then
            verify(userRepository).save(any(User.class));
            verify(imageService).getDefaultImage();
            verify(imageService, never()).saveImage(any());
        }

        @Test
        @DisplayName("실패 - 중복 닉네임")
        void fail_duplicatedNickname() {
            // given
            UserRegisterRequest request = new UserRegisterRequest("new@email.com", "중복닉네임", "password", "USER");
            given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.registerUser(request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATED_NICKNAME));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 중복 이메일")
        void fail_duplicatedEmail() {
            // given
            UserRegisterRequest request = new UserRegisterRequest("dup@email.com", "newUser", "password", "USER");
            given(userRepository.existsByNickname("newUser")).willReturn(false);
            given(userRepository.existsByEmail("dup@email.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.registerUser(request, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATED_EMAIL));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("회원 조회 (getUser)")
    class GetUser {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserDetailResponse response = userService.getUser(userId.toString());

            // then
            assertThat(response.getEmail()).isEqualTo("test@email.com");
            assertThat(response.getNickname()).isEqualTo("테스터");
            assertThat(response.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void fail_notFound() {
            // given
            UUID unknownId = UUID.randomUUID();
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUser(unknownId.toString()))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
        }
    }

    @Nested
    @DisplayName("회원 수정 (updateUser)")
    class UpdateUser {

        @Test
        @DisplayName("성공 - 닉네임 변경")
        void success_nicknameChange() {
            // given
            UserUpdateRequest updateRequest = new UserUpdateRequest("새닉네임");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserDetailResponse response = userService.updateUser(userId.toString(), updateRequest, null);

            // then
            assertThat(response.getNickname()).isEqualTo("새닉네임");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void fail_notFound() {
            // given
            UUID unknownId = UUID.randomUUID();
            UserUpdateRequest updateRequest = new UserUpdateRequest("새닉네임");
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateUser(unknownId.toString(), updateRequest, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 (softDeleteUser)")
    class SoftDeleteUser {

        @Test
        @DisplayName("성공 - deleted 플래그 변경 및 닉네임 변경")
        void success() {
            // given
            String cookieId = userId.toString();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);

            // when
            userService.softDeleteUser(userId.toString(), cookieId, "password");

            // then
            assertThat(user.getDeleted()).isTrue();
            assertThat(user.getNickname()).startsWith("탈퇴_");
        }

        @Test
        @DisplayName("실패 - userId와 cookieId 불일치")
        void fail_userIdMismatch() {
            // given
            String differentCookieId = UUID.randomUUID().toString();

            // when & then
            assertThatThrownBy(() -> userService.softDeleteUser(userId.toString(), differentCookieId, "password"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치")
        void fail_wrongPassword() {
            // given
            String cookieId = userId.toString();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.softDeleteUser(userId.toString(), cookieId, "wrongPassword"))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_PASSWORD));

            assertThat(user.getDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 (changePassword)")
    class ChangePassword {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UserPasswordRequest request = new UserPasswordRequest("currentPw", "newPw");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("currentPw", "encodedPassword")).willReturn(true);
            given(passwordEncoder.encode("newPw")).willReturn("newEncodedPassword");

            // when
            boolean result = userService.changePassword(userId.toString(), request);

            // then
            assertThat(result).isTrue();
            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("실패 - 현재 비밀번호 불일치")
        void fail_wrongCurrentPassword() {
            // given
            UserPasswordRequest request = new UserPasswordRequest("wrongPw", "newPw");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.changePassword(userId.toString(), request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.BAD_PASSWORD));

            assertThat(user.getPassword()).isEqualTo("encodedPassword");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void fail_notFound() {
            // given
            UUID unknownId = UUID.randomUUID();
            UserPasswordRequest request = new UserPasswordRequest("pw", "newPw");
            given(userRepository.findById(unknownId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.changePassword(unknownId.toString(), request))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                            .isEqualTo(ErrorCode.NOT_FOUND_USER));
        }
    }

    @Nested
    @DisplayName("중복 체크 (duplicateCheckUserInfo)")
    class DuplicateCheck {

        @Test
        @DisplayName("이메일 중복 - true 반환")
        void email_duplicated() {
            // given
            given(userRepository.existsByEmail("dup@email.com")).willReturn(true);

            // when
            boolean result = userService.duplicateCheckUserInfo("email", "dup@email.com");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("이메일 미중복 - false 반환")
        void email_notDuplicated() {
            // given
            given(userRepository.existsByEmail("new@email.com")).willReturn(false);

            // when
            boolean result = userService.duplicateCheckUserInfo("email", "new@email.com");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("닉네임 중복 - true 반환")
        void nickname_duplicated() {
            // given
            given(userRepository.existsByNickname("중복닉")).willReturn(true);

            // when
            boolean result = userService.duplicateCheckUserInfo("nickname", "중복닉");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("알 수 없는 info 타입 - false 반환")
        void unknown_type() {
            // when
            boolean result = userService.duplicateCheckUserInfo("unknown", "anything");

            // then
            assertThat(result).isFalse();
        }
    }
}
