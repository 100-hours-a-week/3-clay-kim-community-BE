package kr.kakaotech.community.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import kr.kakaotech.community.auth.jwt.JwtProvider;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseSubscription;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.CourseSubscriptionRepository;
import kr.kakaotech.community.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("performance")
@SpringBootTest(properties = {
        "jwt.expirationtime.accessTtl=1800",
        "jwt.expirationtime.refreshTtl=604800",
        "jwt.secret=test-only-no-sensitive-secret-for-integration-test",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=false"
})
@AutoConfigureMockMvc
class CourseReportSyncPerformanceTest {

    private static final int[] SUBSCRIBER_COUNTS = {0, 100, 500, 1000, 5000, 10000, 50000, 100000};

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtProvider jwtProvider;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired CourseSubscriptionRepository courseSubscriptionRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("구독자 수 증가에 따른 동기 상태 제보 등록 시간과 저장 정합성을 관측한다")
    void reportRegistrationLatencyIncreasesBySubscriberCount() throws Exception {
        List<PerformanceResult> results = new ArrayList<>();

        performReport(prepareScenario(99, 10));

        for (int scenarioIndex = 0; scenarioIndex < SUBSCRIBER_COUNTS.length; scenarioIndex++) {
            int subscriberCount = SUBSCRIBER_COUNTS[scenarioIndex];
            Scenario scenario = prepareScenario(scenarioIndex, subscriberCount);
            long elapsedMs = performReport(scenario);

            assertThat(countReports(scenario.courseId())).isEqualTo(1);
            assertThat(countNotifications(scenario.courseId())).isEqualTo(subscriberCount);
            assertThat(currentStatus(scenario.courseId())).isEqualTo("CONSTRUCTION");

            results.add(new PerformanceResult(subscriberCount, elapsedMs));
        }

        printResults(results);
    }

    private long performReport(Scenario scenario) throws Exception {
        long startedAt = System.nanoTime();
        mockMvc.perform(post("/api/courses/{courseId}/reports", scenario.courseId())
                        .contextPath("/api")
                        .cookie(scenario.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "CONSTRUCTION",
                                "content", "로컬 동기 알림 성능 관측용 제보입니다."
                        ))))
                .andExpect(status().isCreated());
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private Scenario prepareScenario(int scenarioIndex, int subscriberCount) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User reporter = userRepository.saveAndFlush(new User(
                "reporter-" + scenarioIndex + "-" + suffix + "@test.com",
                "password",
                "r" + scenarioIndex + suffix.substring(0, 4),
                "USER"
        ));
        Course course = courseRepository.saveAndFlush(new Course("동기알림성능테스트-" + scenarioIndex));

        List<User> subscribers = IntStream.range(0, subscriberCount)
                .mapToObj(index -> new User(
                        "subscriber-" + scenarioIndex + "-" + index + "-" + suffix + "@test.com",
                        "password",
                        "s" + scenarioIndex + "u" + index,
                        "USER"
                ))
                .toList();
        userRepository.saveAllAndFlush(subscribers);

        List<CourseSubscription> subscriptions = subscribers.stream()
                .map(user -> new CourseSubscription(user, course))
                .toList();
        courseSubscriptionRepository.saveAllAndFlush(subscriptions);

        return new Scenario(course.getId(), accessCookie(reporter));
    }

    private Cookie accessCookie(User user) {
        Cookie cookie = new Cookie("accessToken", jwtProvider.createAccess(user.getId().toString(), "USER"));
        cookie.setPath("/");
        return cookie;
    }

    private long countReports(Integer courseId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM course_reports WHERE course_id = ?",
                Long.class,
                courseId
        );
    }

    private long countNotifications(Integer courseId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications n
                JOIN course_reports cr ON cr.id = n.course_report_id
                WHERE cr.course_id = ?
                """,
                Long.class,
                courseId
        );
    }

    private String currentStatus(Integer courseId) {
        return jdbcTemplate.queryForObject(
                "SELECT current_status FROM courses WHERE id = ?",
                String.class,
                courseId
        );
    }

    private void printResults(List<PerformanceResult> results) {
        System.out.println();
        System.out.println("========== SYNC COURSE REPORT LOCAL PERFORMANCE ==========");
        System.out.println("warm-up request is excluded from results");
        System.out.println("subscribers | elapsed_ms | ms_per_subscriber");
        results.forEach(result -> {
            double msPerSubscriber = result.subscriberCount() == 0
                    ? 0.0
                    : (double) result.elapsedMs() / result.subscriberCount();
            System.out.printf(
                    "%11d | %10d | %.4f%n",
                    result.subscriberCount(),
                    result.elapsedMs(),
                    msPerSubscriber
            );
        });
        System.out.println("==========================================================");
        System.out.println();
    }

    private record Scenario(Integer courseId, Cookie accessToken) {
    }

    private record PerformanceResult(int subscriberCount, long elapsedMs) {
    }
}
