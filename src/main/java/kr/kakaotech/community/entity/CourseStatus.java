package kr.kakaotech.community.entity;

import lombok.Getter;

@Getter
public enum CourseStatus {
    NORMAL("정상"),
    CAUTION("주의"),
    CONSTRUCTION("공사"),
    CLOSED("통제");

    private final String description;

    CourseStatus(String description) {
        this.description = description;
    }
}
