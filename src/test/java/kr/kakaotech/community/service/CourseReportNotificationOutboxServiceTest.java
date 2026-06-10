package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.EventOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseReportNotificationOutboxServiceTest {

    @Mock
    EventOutboxRepository eventOutboxRepository;
    @Mock
    NotificationService notificationService;
    @Mock
    CourseReportRepository courseReportRepository;

    @Test
    @DisplayName("처리할 Outbox 이벤트가 없으면 알림을 생성하지 않는다")
    void processPendingCourseReportCreatedEvent_noPendingEvent() {
        // given
        CourseReportNotificationOutboxService outboxService = new CourseReportNotificationOutboxService(
                eventOutboxRepository,
                notificationService,
                courseReportRepository
        );
        given(eventOutboxRepository.findFirstProcessableForUpdate(
                eq("COURSE_REPORT_CREATED"),
                eq("COURSE_REPORT"),
                eq(EventOutboxStatus.PENDING.name()),
                any()
        )).willReturn(Optional.empty());

        // when
        outboxService.processPendingCourseReportCreatedEvent();

        // then
        verifyNoInteractions(notificationService, courseReportRepository);
    }

    @Test
    @DisplayName("PENDING Outbox 이벤트로 제보 알림을 생성하고 처리 완료 상태로 변경한다")
    void processPendingCourseReportCreatedEvent_success() {
        // given
        CourseReportNotificationOutboxService outboxService = new CourseReportNotificationOutboxService(
                eventOutboxRepository,
                notificationService,
                courseReportRepository
        );
        EventOutbox outbox = new EventOutbox(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                99L,
                "{}"
        );
        CourseReport report = new CourseReport(
                new Course("한강종주"),
                new User("report@test.com", "password", "reporter", "USER"),
                CourseReportType.CONSTRUCTION,
                "강변 진입로 일부 공사 중입니다."
        );
        ReflectionTestUtils.setField(report, "id", 99L);

        given(eventOutboxRepository.findFirstProcessableForUpdate(
                eq("COURSE_REPORT_CREATED"),
                eq("COURSE_REPORT"),
                eq(EventOutboxStatus.PENDING.name()),
                any()
        )).willReturn(Optional.of(outbox));
        given(courseReportRepository.findById(99L)).willReturn(Optional.of(report));

        // when
        outboxService.processPendingCourseReportCreatedEvent();

        // then
        verify(notificationService).createCourseReportNotifications(report, outbox.getEventId());
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSED);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Outbox 처리 실패 시 retryCount를 증가시키고 다음 재시도 시간을 기록한다")
    void processPendingCourseReportCreatedEvent_retry() {
        // given
        CourseReportNotificationOutboxService outboxService = new CourseReportNotificationOutboxService(
                eventOutboxRepository,
                notificationService,
                courseReportRepository
        );
        EventOutbox outbox = new EventOutbox(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                99L,
                "{}"
        );
        CourseReport report = new CourseReport(
                new Course("한강종주"),
                new User("report@test.com", "password", "reporter", "USER"),
                CourseReportType.CONSTRUCTION,
                "강변 진입로 일부 공사 중입니다."
        );
        ReflectionTestUtils.setField(report, "id", 99L);

        given(eventOutboxRepository.findFirstProcessableForUpdate(
                eq("COURSE_REPORT_CREATED"),
                eq("COURSE_REPORT"),
                eq(EventOutboxStatus.PENDING.name()),
                any()
        )).willReturn(Optional.of(outbox));
        given(courseReportRepository.findById(99L)).willReturn(Optional.of(report));
        willThrow(new RuntimeException("notification failure"))
                .given(notificationService)
                .createCourseReportNotifications(report, outbox.getEventId());

        // when
        outboxService.processPendingCourseReportCreatedEvent();

        // then
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getNextRetryAt()).isNotNull();
    }

    @Test
    @DisplayName("최대 재시도 횟수를 넘기면 Outbox 이벤트를 FAILED로 격리한다")
    void processPendingCourseReportCreatedEvent_failed() {
        // given
        CourseReportNotificationOutboxService outboxService = new CourseReportNotificationOutboxService(
                eventOutboxRepository,
                notificationService,
                courseReportRepository
        );
        EventOutbox outbox = new EventOutbox(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                99L,
                "{}"
        );
        ReflectionTestUtils.setField(outbox, "retryCount", 2);
        CourseReport report = new CourseReport(
                new Course("한강종주"),
                new User("report@test.com", "password", "reporter", "USER"),
                CourseReportType.CONSTRUCTION,
                "강변 진입로 일부 공사 중입니다."
        );
        ReflectionTestUtils.setField(report, "id", 99L);

        given(eventOutboxRepository.findFirstProcessableForUpdate(
                eq("COURSE_REPORT_CREATED"),
                eq("COURSE_REPORT"),
                eq(EventOutboxStatus.PENDING.name()),
                any()
        )).willReturn(Optional.of(outbox));
        given(courseReportRepository.findById(99L)).willReturn(Optional.of(report));
        willThrow(new RuntimeException("notification failure"))
                .given(notificationService)
                .createCourseReportNotifications(report, outbox.getEventId());

        // when
        outboxService.processPendingCourseReportCreatedEvent();

        // then
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getNextRetryAt()).isNull();
    }
}
