package kr.kakaotech.community.service;

import kr.kakaotech.community.dto.request.ReportRegisterRequest;
import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportStatus;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.CourseStatus;
import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.CourseRepository;
import kr.kakaotech.community.repository.EventOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseReportServiceTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    UserLookupService userLookupService;
    @Mock
    CourseReportRepository courseReportRepository;
    @Mock
    EventOutboxRepository eventOutboxRepository;

    CourseReportService courseReportService;

    @BeforeEach
    void setUp() {
        courseReportService = new CourseReportService(
                courseRepository,
                userLookupService,
                courseReportRepository,
                eventOutboxRepository
        );
    }

    @Test
    @DisplayName("상태 제보를 저장하고 코스 상태를 갱신한다")
    void registerReport_success() {
        // given
        Integer courseId = 1;
        UUID userId = UUID.randomUUID();
        Course course = new Course("한강종주");
        User user = new User("report@test.com", "password", "reporter", "USER");
        ReportRegisterRequest request = new ReportRegisterRequest();
        ReflectionTestUtils.setField(request, "type", CourseReportType.CONSTRUCTION);
        ReflectionTestUtils.setField(request, "content", "강변 진입로 일부 공사 중입니다.");

        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(userLookupService.getRequiredUser(userId)).willReturn(user);
        given(courseReportRepository.save(any(CourseReport.class))).willAnswer(invocation -> {
            CourseReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 99L);
            return report;
        });

        // when
        CourseReport savedReport = courseReportService.registerReport(courseId, request, userId);

        // then
        ArgumentCaptor<CourseReport> reportCaptor = ArgumentCaptor.forClass(CourseReport.class);
        verify(courseReportRepository).save(reportCaptor.capture());
        CourseReport report = reportCaptor.getValue();
        assertThat(savedReport).isEqualTo(report);
        assertThat(report.getCourse()).isEqualTo(course);
        assertThat(report.getUser()).isEqualTo(user);
        assertThat(report.getType()).isEqualTo(CourseReportType.CONSTRUCTION);
        assertThat(report.getContent()).isEqualTo("강변 진입로 일부 공사 중입니다.");
        assertThat(report.getStatus()).isEqualTo(CourseReportStatus.ACTIVE);
        assertThat(course.getCurrentStatus()).isEqualTo(CourseStatus.CONSTRUCTION);

        ArgumentCaptor<EventOutbox> outboxCaptor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxRepository).save(outboxCaptor.capture());
        EventOutbox outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventId()).isNotNull();
        assertThat(outbox.getEventType()).isEqualTo("COURSE_REPORT_CREATED");
        assertThat(outbox.getAggregateType()).isEqualTo("COURSE_REPORT");
        assertThat(outbox.getAggregateId()).isEqualTo(99L);
        assertThat(outbox.getPayload()).isEqualTo("{}");
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("존재하지 않는 코스면 제보를 저장하지 않는다")
    void registerReport_fail_courseNotFound() {
        // given
        Integer courseId = 1;
        UUID userId = UUID.randomUUID();
        ReportRegisterRequest request = new ReportRegisterRequest();
        ReflectionTestUtils.setField(request, "type", CourseReportType.CAUTION);
        ReflectionTestUtils.setField(request, "content", "노면 파손이 있습니다.");

        given(courseRepository.findById(courseId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> courseReportService.registerReport(courseId, request, userId))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND_COURSE));
        verify(userLookupService, never()).getRequiredUser(any());
        verify(courseReportRepository, never()).save(any());
        verify(eventOutboxRepository, never()).save(any());
    }
}
