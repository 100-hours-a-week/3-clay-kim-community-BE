package kr.kakaotech.community.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import kr.kakaotech.community.auth.jwt.JwtProvider;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportStatus;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.CourseStatus;
import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.EventOutboxRepository;
import kr.kakaotech.community.repository.NotificationRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.expirationtime.accessTtl=1800",
        "jwt.expirationtime.refreshTtl=604800",
        "jwt.secret=test-only-no-sensitive-secret-for-integration-test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseReportIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtProvider jwtProvider;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseReportRepository courseReportRepository;
    @Autowired CourseSubscriptionRepository courseSubscriptionRepository;
    @Autowired EventOutboxRepository eventOutboxRepository;
    @Autowired NotificationRepository notificationRepository;
    @PersistenceContext EntityManager em;

    @Test
    @DisplayName("POST /courses/{courseId}/reports - 제보, 코스 상태, Outbox 이벤트를 저장한다")
    void registerCourseReport_success() throws Exception {
        // given
        User user = userRepository.saveAndFlush(new User("course-report@test.com", "password", "reporter", "USER"));
        User firstSubscriber = userRepository.saveAndFlush(new User("course-report-sub1@test.com", "password", "subone", "USER"));
        User secondSubscriber = userRepository.saveAndFlush(new User("course-report-sub2@test.com", "password", "subtwo", "USER"));
        Course course = courseRepository.saveAndFlush(new Course("한강종주"));
        courseSubscriptionRepository.saveAndFlush(new CourseSubscription(firstSubscriber, course));
        courseSubscriptionRepository.saveAndFlush(new CourseSubscription(secondSubscriber, course));
        Cookie accessToken = new Cookie("accessToken", jwtProvider.createAccess(user.getId().toString(), "USER"));
        accessToken.setPath("/");

        // when & then
        mockMvc.perform(post("/api/courses/{courseId}/reports", course.getId())
                        .contextPath("/api")
                        .cookie(accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CONSTRUCTION",
                                "content", "강변 진입로 일부 공사 중입니다."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("코스 상태 제보 등록 성공"));

        flushAndClear();

        List<CourseReport> reports = courseReportRepository.findAll();
        assertThat(reports).hasSize(1);
        CourseReport report = reports.get(0);
        assertThat(report.getCourse().getId()).isEqualTo(course.getId());
        assertThat(report.getUser().getId()).isEqualTo(user.getId());
        assertThat(report.getType()).isEqualTo(CourseReportType.CONSTRUCTION);
        assertThat(report.getContent()).isEqualTo("강변 진입로 일부 공사 중입니다.");
        assertThat(report.getStatus()).isEqualTo(CourseReportStatus.ACTIVE);

        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentStatus()).isEqualTo(CourseStatus.CONSTRUCTION);

        List<EventOutbox> outboxes = eventOutboxRepository.findAll();
        assertThat(outboxes).hasSize(1);
        EventOutbox outbox = outboxes.get(0);
        assertThat(outbox.getEventId()).isNotNull();
        assertThat(outbox.getEventType()).isEqualTo("COURSE_REPORT_CREATED");
        assertThat(outbox.getAggregateType()).isEqualTo("COURSE_REPORT");
        assertThat(outbox.getAggregateId()).isEqualTo(report.getId());
        assertThat(outbox.getPayload()).isEqualTo("{}");
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
