package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class CourseReportCommandServiceTest {

    @Mock
    CourseReportService courseReportService;
    @Mock
    NotificationService notificationService;

    @Test
    @DisplayName("제보를 저장한 뒤 저장된 제보로 구독자 알림을 생성한다")
    void registerReport_success() {
        // given
        Integer courseId = 1;
        UUID userId = UUID.randomUUID();
        ReportRegisterRequest request = new ReportRegisterRequest();
        CourseReport report = new CourseReport(
                new Course("한강종주"),
                new User("report@test.com", "password", "reporter", "USER"),
                CourseReportType.CAUTION,
                "노면 파손이 있습니다."
        );
        CourseReportCommandService commandService = new CourseReportCommandService(
                courseReportService,
                notificationService
        );

        given(courseReportService.registerReport(courseId, request, userId)).willReturn(report);

        // when
        commandService.registerReport(courseId, request, userId);

        // then
        InOrder inOrder = inOrder(courseReportService, notificationService);
        inOrder.verify(courseReportService).registerReport(courseId, request, userId);
        inOrder.verify(notificationService).createCourseReportNotifications(report);
    }
}
