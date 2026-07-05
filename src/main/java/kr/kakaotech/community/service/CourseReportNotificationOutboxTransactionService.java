package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private static final String INSERT_NOTIFICATIONS_SQL = """
            INSERT INTO notifications (
                user_id, course_report_id, event_id, title, content, is_read, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE event_id = event_id
            """;

    private final EventOutboxRepository eventOutboxRepository;
    private final JdbcTemplate jdbcTemplate;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertNotificationChunk(
            List<UUID> userIds,
            Long courseReportId,
            UUID eventId,
            String title,
            String content
    ) {
        byte[] eventIdBytes = uuidToBytes(eventId);
        Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());
        List<Object[]> batchArgs = userIds.stream()
                .map(userId -> new Object[]{
                        uuidToBytes(userId), courseReportId, eventIdBytes, title, content, false, createdAt
                })
                .toList();

        jdbcTemplate.batchUpdate(INSERT_NOTIFICATIONS_SQL, batchArgs);
    }

    private boolean isCurrentClaim(EventOutbox eventOutbox, Claim claim) {
        return eventOutbox.getStatus() == EventOutboxStatus.PROCESSING
                && claim.processingStartedAt().equals(eventOutbox.getProcessingStartedAt());
    }

    private long calculateRetryDelaySeconds(int retryCount) {
        return BASE_RETRY_DELAY_SECONDS * (1L << Math.max(0, retryCount - 1));
    }

    private byte[] uuidToBytes(UUID uuid) {
        return ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public record Claim(Long outboxId, UUID eventId, Long aggregateId, LocalDateTime processingStartedAt) {
    }

    public record Failure(EventOutboxStatus status, int retryCount, LocalDateTime nextRetryAt) {
    }
}
