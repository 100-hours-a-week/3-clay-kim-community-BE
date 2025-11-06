package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.UserPasswordRequest;
import kr.kakaotech.community.dto.request.UserRegisterRequest;
import kr.kakaotech.community.dto.request.UserUpdateRequest;
import kr.kakaotech.community.dto.response.UserDetailResponse;
import kr.kakaotech.community.entity.Image;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.ImageRepository;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;
    private final String DEFAULT_IMAGE = "default";
    private final ImageRepository imageRepository;

    /**
     * 회원가입
     * 이미지 저장 후 유저를 저장합니다.
     *
     * @param registerRequest
     */
    @Transactional
    public void registerUser(UserRegisterRequest registerRequest, MultipartFile image) {
        // nickname 검증
        if (userRepository.existsByNickname(registerRequest.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }

        User user = new User(
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getNickname(),
                registerRequest.getRole()
        );

        // 이미지 저장
        if (image != null && !image.isEmpty()) {
            Image imageEntity = imageService.saveImage(image);
            user.addImage(imageEntity);
        }

        userRepository.save(user);
    }

    /**
     * 단일 사용자 불러오기
     *
     * @param userId
     * @return
     */
    public UserDetailResponse getUser(String userId) {
        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        return new UserDetailResponse(
                getUser.getId().toString(),
                getUser.getEmail(),
                getUser.getNickname(),
                getUser.getDeleted(),
                getUser.getRole().toString()
        );
    }

    /**
     * 관리자를 위한 모든 유저 불러오기
     * Spring의 Pageable을 이용해 Pagination을 진행합니다.
     *
     * @param pageable
     * @return
     */
    public Page<UserDetailResponse> getUserPage(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);

        return userPage.map(getUser -> new UserDetailResponse(
                getUser.getId().toString(),
                getUser.getEmail(), getUser.getNickname(),
                getUser.getDeleted(),
                getUser.getRole().toString()
        ));
    }

    /**
     * User 업데이트
     * User의 updateUser()를 통해 변경사항만 업데이트합니다.
     */
    @Transactional
    public UserDetailResponse updateUser(String userId, UserUpdateRequest userUpdateRequest) {
        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        getUser.updateUser(userUpdateRequest);

        return new UserDetailResponse(
                getUser.getId().toString(),
                getUser.getEmail(),
                getUser.getNickname(),
                getUser.getDeleted(),
                getUser.getRole().toString()
        );
    }

    /**
     * Soft Delete 유저 삭제
     * User의 deleted를 true로 변경, deletedAt을 추가합니다.
     *
     * @param userId
     */
    @Transactional
    public void softDeleteUser(String userId) {
        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        getUser.deleteUser();
    }

    /**
     * Hard Delete 메서드
     * 스케쥴같은 방식으로 deleted가 true인데 한달이 지났으면 삭제
     * cascade 로 이미지도 같이 삭제
     *
     * @param userId
     */
    @Transactional
    public void hardDeleteUser(String userId) {
        User getUser = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        userRepository.delete(getUser);
    }

    public boolean duplicateCheckUserInfo(String info, String userInput) {
        return switch (info) {
            case "email" -> userRepository.existsByEmail(userInput);
            case "nickname" -> userRepository.existsByNickname(userInput);
            default -> false;
        };
    }

    @Transactional
    public boolean changePassword(String userId, UserPasswordRequest userPasswordRequest) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_FOUND_USER));

        // 비밀번호 에러보다는 정보를 알려주지않기 위해 실패했다는 false 만 반환
        if (!user.getPassword().equals(passwordEncoder.encode(userPasswordRequest.getCurrentPassword()))) {
            throw new CustomException(ErrorCode.BAD_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(userPasswordRequest.getNewPassword()));
        return true;
    }
}
