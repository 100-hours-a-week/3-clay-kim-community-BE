package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseReportCommandServiceTest {

    @Mock
    CourseReportService courseReportService;

    @Test
    @DisplayName("제보 등록 요청을 제보 서비스에 위임한다")
    void registerReport_success() {
        // given
        Integer courseId = 1;
        UUID userId = UUID.randomUUID();
        ReportRegisterRequest request = new ReportRegisterRequest();
        CourseReportCommandService commandService = new CourseReportCommandService(courseReportService);

        // when
        commandService.registerReport(courseId, request, userId);

        // then
        verify(courseReportService).registerReport(courseId, request, userId);
    }
}
