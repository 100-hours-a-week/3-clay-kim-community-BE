package kr.kakaotech.community.scheduler;

import kr.kakaotech.community.service.CourseReportNotificationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.scheduler.course-report-notification.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Service
public class CourseReportNotificationOutboxScheduler {

    private final CourseReportNotificationOutboxService courseReportNotificationOutboxService;

    @Scheduled(fixedDelay = 1000)
    public void processCourseReportNotificationOutbox() {
        courseReportNotificationOutboxService.processPendingCourseReportCreatedEvent();
    }
}
