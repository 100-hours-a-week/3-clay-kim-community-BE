package kr.kakaotech.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@Getter
public class UserUpdateRequest {
    private String nickname;
    private MultipartFile profileImage;
}
