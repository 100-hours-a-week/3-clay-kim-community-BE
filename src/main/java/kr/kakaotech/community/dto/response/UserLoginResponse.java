package kr.kakaotech.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserLoginResponse {
    private String nickname;
    private String userEmail;
    private String userId;
}
