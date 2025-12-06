package kr.kakaotech.community.entity;

import lombok.Getter;

@Getter
public enum PostType {
    IN_PROGRESS("진행중"),
    COMPLETED("완료");

    private final String description;

    PostType(String description) {
        this.description = description;
    }

}
