package kr.kakaotech.community.dto.response;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String name;
    private CourseStatus currentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getCurrentStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
