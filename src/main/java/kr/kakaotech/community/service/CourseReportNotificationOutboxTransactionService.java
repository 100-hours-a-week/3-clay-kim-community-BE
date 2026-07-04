package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CourseReportNotificationOutboxTransactionService {
    private static final String COURSE_REPORT_CREATED_EVENT_TYPE = "COURSE_REPORT_CREATED";
    private static final String COURSE_REPORT_AGGREGATE_TYPE = "COURSE_REPORT";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long BASE_RETRY_DELAY_SECONDS = 5;
    private static final long PROCESSING_LEASE_MINUTES = 5;

    private final EventOutboxRepository eventOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Claim> claimNext() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        return eventOutboxRepository.findFirstProcessableForUpdate(
                        COURSE_REPORT_CREATED_EVENT_TYPE,
                        COURSE_REPORT_AGGREGATE_TYPE,
                        EventOutboxStatus.PENDING.name(),
                        EventOutboxStatus.PROCESSING.name(),
                        now,
                        now.minusMinutes(PROCESSING_LEASE_MINUTES)
                )
                .map(eventOutbox -> {
                    eventOutbox.markProcessing(now);
                    return new Claim(
                            eventOutbox.getId(),
                            eventOutbox.getEventId(),
                            eventOutbox.getAggregateId(),
                            now
                    );
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(Claim claim) {
        EventOutbox eventOutbox = eventOutboxRepository.findByIdForUpdate(claim.outboxId()).orElseThrow();
        if (!isCurrentClaim(eventOutbox, claim)) {
            return false;
        }

        eventOutbox.markProcessed();
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Failure> recordFailure(Claim claim) {
        EventOutbox eventOutbox = eventOutboxRepository.findByIdForUpdate(claim.outboxId()).orElseThrow();
        if (!isCurrentClaim(eventOutbox, claim)) {
            return Optional.empty();
        }

        int nextRetryCount = eventOutbox.getRetryCount() + 1;
        eventOutbox.recordFailure(
                MAX_RETRY_COUNT,
                LocalDateTime.now().plusSeconds(calculateRetryDelaySeconds(nextRetryCount))
        );
        return Optional.of(new Failure(
                eventOutbox.getStatus(),
                eventOutbox.getRetryCount(),
                eventOutbox.getNextRetryAt()
        ));
    }

    private boolean isCurrentClaim(EventOutbox eventOutbox, Claim claim) {
        return eventOutbox.getStatus() == EventOutboxStatus.PROCESSING
                && claim.processingStartedAt().equals(eventOutbox.getProcessingStartedAt());
    }

    private long calculateRetryDelaySeconds(int retryCount) {
        return BASE_RETRY_DELAY_SECONDS * (1L << Math.max(0, retryCount - 1));
    }

    public record Claim(Long outboxId, UUID eventId, Long aggregateId, LocalDateTime processingStartedAt) {
    }

    public record Failure(EventOutboxStatus status, int retryCount, LocalDateTime nextRetryAt) {
    }
}
