package kr.kakaotech.community.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.kakaotech.community.entity.CourseReportType;
import lombok.Getter;

@Schema(name = "ReportRegisterRequest", description = "코스 상태 제보 등록 요청")
@Getter
public class ReportRegisterRequest {
    @NotNull(message = "제보 타입을 선택해주세요.")
    @JsonAlias("reportType")
    @Schema(
            description = "제보할 코스 상태",
            example = "CONSTRUCTION",
            allowableValues = {"NORMAL", "CAUTION", "CONSTRUCTION", "CLOSED"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CourseReportType type;

    @NotBlank(message = "제보 내용을 입력해주세요.")
    @Size(max = 1000, message = "제보 내용은 1000자 이하로 작성해주세요.")
    @Schema(
            description = "상태 제보 상세 내용",
            example = "강변 진입로 일부 공사 중입니다. 우회가 필요합니다.",
            maxLength = 1000,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String content;
}
