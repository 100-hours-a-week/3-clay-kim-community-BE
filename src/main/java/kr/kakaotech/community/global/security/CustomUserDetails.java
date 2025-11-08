package kr.kakaotech.community.global.security;

import kr.kakaotech.community.entity.User;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public String getUserId() {
        return user.getId().toString();
    }
    public String getNickname() {
        return user.getNickname();
    }

    public String getPassword() {
        return user.getPassword();
    }

    public String getUsername() {
        return user.getEmail();
    }
}
