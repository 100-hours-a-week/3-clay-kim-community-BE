package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.Course;
import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.CourseReportType;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.entity.User;
import kr.kakaotech.community.repository.CourseReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourseReportNotificationOutboxServiceTest {

    @Mock
    CourseReportNotificationOutboxTransactionService transactionService;
    @Mock
    NotificationService notificationService;
    @Mock
    CourseReportRepository courseReportRepository;

    @Test
    @DisplayName("처리할 Outbox 이벤트가 없으면 알림을 생성하지 않는다")
    void processPendingCourseReportCreatedEvent_noPendingEvent() {
        var outboxService = outboxService();
        given(transactionService.claimNext()).willReturn(Optional.empty());

        outboxService.processPendingCourseReportCreatedEvent();

        verifyNoInteractions(notificationService, courseReportRepository);
    }

    @Test
    @DisplayName("claim한 이벤트의 알림 생성 후 별도 트랜잭션에 완료 기록을 위임한다")
    void processPendingCourseReportCreatedEvent_success() {
        var outboxService = outboxService();
        var claim = claim();
        CourseReport report = report();
        given(transactionService.claimNext()).willReturn(Optional.of(claim));
        given(courseReportRepository.findByIdWithCourse(claim.aggregateId())).willReturn(Optional.of(report));
        given(transactionService.markProcessed(claim)).willReturn(true);

        outboxService.processPendingCourseReportCreatedEvent();

        verify(notificationService).createCourseReportNotifications(report, claim.eventId());
        verify(transactionService).markProcessed(claim);
    }

    @Test
    @DisplayName("알림 처리 실패 시 별도 트랜잭션에 실패 기록을 위임한다")
    void processPendingCourseReportCreatedEvent_failure() {
        var outboxService = outboxService();
        var claim = claim();
        CourseReport report = report();
        RuntimeException failure = new RuntimeException("notification failure");
        given(transactionService.claimNext()).willReturn(Optional.of(claim));
        given(courseReportRepository.findByIdWithCourse(claim.aggregateId())).willReturn(Optional.of(report));
        willThrow(failure).given(notificationService).createCourseReportNotifications(report, claim.eventId());
        given(transactionService.recordFailure(claim)).willReturn(Optional.of(
                new CourseReportNotificationOutboxTransactionService.Failure(
                        EventOutboxStatus.PENDING,
                        1,
                        LocalDateTime.now().plusSeconds(5)
                )
        ));

        outboxService.processPendingCourseReportCreatedEvent();

        verify(transactionService).recordFailure(claim);
    }

    private CourseReportNotificationOutboxService outboxService() {
        return new CourseReportNotificationOutboxService(
                transactionService,
                notificationService,
                courseReportRepository
        );
    }

    private CourseReportNotificationOutboxTransactionService.Claim claim() {
        return new CourseReportNotificationOutboxTransactionService.Claim(
                1L,
                UUID.randomUUID(),
                99L,
                LocalDateTime.now()
        );
    }

    private CourseReport report() {
        CourseReport report = new CourseReport(
                new Course("한강종주"),
                new User("report@test.com", "password", "reporter", "USER"),
                CourseReportType.CONSTRUCTION,
                "강변 진입로 일부 공사 중입니다."
        );
        ReflectionTestUtils.setField(report, "id", 99L);
        return report;
    }
}
