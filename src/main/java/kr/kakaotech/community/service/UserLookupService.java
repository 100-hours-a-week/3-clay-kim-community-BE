package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLookupService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getRequiredUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    @Transactional(readOnly = true)
    public void requireExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
    }

    public User getReference(UUID userId) {
        return userRepository.getReferenceById(userId);
    }
}
