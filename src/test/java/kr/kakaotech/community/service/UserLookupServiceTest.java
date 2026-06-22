package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserLookupService userLookupService;

    @Test
    @DisplayName("필수 사용자 조회 - 성공")
    void getRequiredUser_success() {
        // given
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userLookupService.getRequiredUser(userId);

        // then
        assertThat(result).isEqualTo(user);
    }

    @Test
    @DisplayName("필수 사용자 조회 - 사용자가 없으면 NOT_FOUND_USER")
    void getRequiredUser_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userLookupService.getRequiredUser(userId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND_USER));
    }

    @Test
    @DisplayName("사용자 존재 검증 - 성공")
    void requireExists_success() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsById(userId)).willReturn(true);

        // when
        userLookupService.requireExists(userId);

        // then
        verify(userRepository).existsById(userId);
    }

    @Test
    @DisplayName("사용자 존재 검증 - 사용자가 없으면 NOT_FOUND_USER")
    void requireExists_userNotFound() {
        // given
        UUID userId = UUID.randomUUID();
        given(userRepository.existsById(userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userLookupService.requireExists(userId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND_USER));
    }

    @Test
    @DisplayName("사용자 프록시 조회 - Repository reference 호출 위임")
    void getReference_delegatesRepositoryReference() {
        // given
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        given(userRepository.getReferenceById(userId)).willReturn(user);

        // when
        User result = userLookupService.getReference(userId);

        // then
        assertThat(result).isEqualTo(user);
        verify(userRepository).getReferenceById(userId);
    }
}
