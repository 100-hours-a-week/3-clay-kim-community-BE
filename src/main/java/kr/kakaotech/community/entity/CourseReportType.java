package kr.kakaotech.community.entity;

import lombok.Getter;

@Getter
public enum CourseReportType {
    NORMAL("정상"),
    CAUTION("주의"),
    CONSTRUCTION("공사"),
    CLOSED("통제");

    private final String description;

    CourseReportType(String description) {
        this.description = description;
    }

    public CourseStatus toCourseStatus() {
        return CourseStatus.valueOf(name());
    }
}
