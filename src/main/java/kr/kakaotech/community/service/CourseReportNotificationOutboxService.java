package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.CourseReport;
import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.CourseReportRepository;
import kr.kakaotech.community.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class CourseReportNotificationOutboxService {
    private static final String COURSE_REPORT_CREATED_EVENT_TYPE = "COURSE_REPORT_CREATED";
    private static final String COURSE_REPORT_AGGREGATE_TYPE = "COURSE_REPORT";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY_SECONDS = 5;

    private final EventOutboxRepository eventOutboxRepository;
    private final NotificationService notificationService;
    private final CourseReportRepository courseReportRepository;

    @Transactional
    public void processPendingCourseReportCreatedEvent() {
        Optional<EventOutbox> pendingEvent = eventOutboxRepository.findFirstProcessableForUpdate(
                COURSE_REPORT_CREATED_EVENT_TYPE,
                COURSE_REPORT_AGGREGATE_TYPE,
                EventOutboxStatus.PENDING.name(),
                LocalDateTime.now()
        );

        if (pendingEvent.isEmpty()) {
            return;
        }

        EventOutbox eventOutbox = pendingEvent.get();
        try {
            CourseReport courseReport = courseReportRepository.findById(eventOutbox.getAggregateId()).orElseThrow();

            notificationService.createCourseReportNotifications(courseReport, eventOutbox.getEventId());
            eventOutbox.markProcessed();
        } catch (RuntimeException e) {
            recordFailure(eventOutbox);
            logFailure(eventOutbox, e);
        }
    }

    private void recordFailure(EventOutbox eventOutbox) {
        int nextRetryCount = eventOutbox.getRetryCount() + 1;
        eventOutbox.recordFailure(
                MAX_RETRY_COUNT,
                LocalDateTime.now().plusSeconds(calculateRetryDelaySeconds(nextRetryCount))
        );
    }

    private long calculateRetryDelaySeconds(int retryCount) {
        return BASE_RETRY_DELAY_SECONDS * (1L << Math.max(0, retryCount - 1));
    }

    private void logFailure(EventOutbox eventOutbox, RuntimeException e) {
        if (eventOutbox.getStatus() == EventOutboxStatus.FAILED) {
            log.error(
                    "Course report notification outbox failed permanently. outboxId={}, eventId={}, eventType={}, aggregateType={}, aggregateId={}, retryCount={}",
                    eventOutbox.getId(),
                    eventOutbox.getEventId(),
                    eventOutbox.getEventType(),
                    eventOutbox.getAggregateType(),
                    eventOutbox.getAggregateId(),
                    eventOutbox.getRetryCount(),
                    e
            );
            return;
        }

        log.warn(
                "Course report notification outbox processing failed. outboxId={}, eventId={}, eventType={}, aggregateType={}, aggregateId={}, retryCount={}, nextRetryAt={}",
                eventOutbox.getId(),
                eventOutbox.getEventId(),
                eventOutbox.getEventType(),
                eventOutbox.getAggregateType(),
                eventOutbox.getAggregateId(),
                eventOutbox.getRetryCount(),
                eventOutbox.getNextRetryAt(),
                e
        );
    }
}
