package kr.kakaotech.community.integration;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.NotificationRepository;
import kr.kakaotech.community.repository.UserRepository;
import kr.kakaotech.community.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.expirationtime.accessTtl=1800",
        "jwt.expirationtime.refreshTtl=604800",
        "jwt.secret=test-only-no-sensitive-secret-for-integration-test"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CourseReportNotificationChunkIntegrationTest {

    @Autowired
    NotificationService notificationService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    CourseRepository courseRepository;
    @Autowired
    CourseReportRepository courseReportRepository;
    @Autowired
    CourseSubscriptionRepository courseSubscriptionRepository;
    @Autowired
    NotificationRepository notificationRepository;

    @Test
    @DisplayName("projection 조회와 JDBC batch 재처리는 구독자별 알림을 한 건만 남긴다")
    void createCourseReportNotifications_isIdempotent() {
        User reporter = userRepository.save(new User("reporter@test.com", "password", "reporter", "USER"));
        User firstSubscriber = userRepository.save(new User("first@test.com", "password", "first", "USER"));
        User secondSubscriber = userRepository.save(new User("second@test.com", "password", "second", "USER"));
        Course course = courseRepository.save(new Course("한강종주"));
        CourseReport report = courseReportRepository.save(new CourseReport(
                course,
                reporter,
                CourseReportType.CLOSED,
                "진입로가 통제되었습니다."
        ));
        courseSubscriptionRepository.saveAll(List.of(
                new CourseSubscription(firstSubscriber, course),
                new CourseSubscription(secondSubscriber, course)
        ));
        UUID eventId = UUID.randomUUID();

        notificationService.createCourseReportNotifications(report, eventId);
        notificationService.createCourseReportNotifications(report, eventId);

        assertThat(notificationRepository.findAll())
                .filteredOn(notification -> notification.getEventId().equals(eventId))
                .hasSize(2);
        assertThat(notificationRepository.countByUser_IdAndCourseReport_Id(firstSubscriber.getId(), report.getId()))
                .isOne();
        assertThat(notificationRepository.countByUser_IdAndCourseReport_Id(secondSubscriber.getId(), report.getId()))
                .isOne();
    }
}
