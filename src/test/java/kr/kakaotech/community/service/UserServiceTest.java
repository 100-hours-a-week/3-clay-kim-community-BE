//package kr.kakaotech.community.service;
//
//import kr.kakaotech.community.dto.request.UserRegisterRequest;
//import kr.kakaotech.community.entity.Image;
//import kr.kakaotech.community.entity.User;
//import kr.kakaotech.community.repository.UserRepository;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.web.multipart.MultipartFile;
//
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @InjectMocks
//    private UserService userService;
//
//    @Test
//    @DisplayName("회원 등록 테스트입니다.")
//    void registerUserTest() {
//        // given
//        UserRegisterRequest registerRequest = new UserRegisterRequest("clay@clay.com", "clay123", "clay", "USER");
//
//        when(userRepository.existsByNickname(registerRequest.getNickname())).thenReturn(false);
//        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
//        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("password");
//
//        MultipartFile multipartFile = null;
//
//        // when
//        userService.registerUser(registerRequest, multipartFile);
//
//        // then
//        verify(userRepository, times(1)).save(any(User.class));
//    }
//
//}