package kr.kakaotech.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(name = "CourseResponse", description = "코스 조회 응답")
@Getter
@AllArgsConstructor
public class CourseResponse {
    @Schema(description = "코스 ID", example = "1")
    private Integer id;

    @Schema(description = "코스명", example = "한강종주")
    private String name;

    @Schema(
            description = "현재 코스 상태",
            example = "NORMAL",
            allowableValues = {"NORMAL", "CAUTION", "CONSTRUCTION", "CLOSED"}
    )
    private CourseStatus currentStatus;

    @Schema(description = "코스 생성 시각", example = "2026-06-08T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "코스 수정 시각", example = "2026-06-08T11:00:00")
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
