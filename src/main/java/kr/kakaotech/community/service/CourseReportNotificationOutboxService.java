package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.CourseReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class CourseReportNotificationOutboxService {
    private final CourseReportNotificationOutboxTransactionService transactionService;
    private final NotificationService notificationService;
    private final CourseReportRepository courseReportRepository;

    public void processPendingCourseReportCreatedEvent() {
        var claim = transactionService.claimNext();
        if (claim.isEmpty()) {
            return;
        }

        var claimedEvent = claim.get();
        try {
            CourseReport courseReport = courseReportRepository.findByIdWithCourse(claimedEvent.aggregateId()).orElseThrow();

            notificationService.createCourseReportNotifications(courseReport, claimedEvent.eventId());
            if (!transactionService.markProcessed(claimedEvent)) {
                log.warn("Ignored completion from expired outbox claim. outboxId={}, eventId={}",
                        claimedEvent.outboxId(), claimedEvent.eventId());
            }
        } catch (RuntimeException e) {
            transactionService.recordFailure(claimedEvent)
                    .ifPresentOrElse(
                            failure -> logFailure(claimedEvent, failure, e),
                            () -> log.warn("Ignored failure from expired outbox claim. outboxId={}, eventId={}",
                                    claimedEvent.outboxId(), claimedEvent.eventId(), e)
                    );
        }
    }

    private void logFailure(
            CourseReportNotificationOutboxTransactionService.Claim claim,
            CourseReportNotificationOutboxTransactionService.Failure failure,
            RuntimeException e
    ) {
        if (failure.status() == EventOutboxStatus.FAILED) {
            log.error(
                    "Course report notification outbox failed permanently. outboxId={}, eventId={}, aggregateId={}, retryCount={}",
                    claim.outboxId(),
                    claim.eventId(),
                    claim.aggregateId(),
                    failure.retryCount(),
                    e
            );
            return;
        }

        log.warn(
                "Course report notification outbox processing failed. outboxId={}, eventId={}, aggregateId={}, retryCount={}, nextRetryAt={}",
                claim.outboxId(),
                claim.eventId(),
                claim.aggregateId(),
                failure.retryCount(),
                failure.nextRetryAt(),
                e
        );
    }
}
