package kr.kakaotech.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserPasswordRequest {
    String currentPassword;
    String newPassword;
}
