package kr.kakaotech.community.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity(name = "event_outbox")
@Table(name = "event_outbox", indexes = {
        @Index(name = "idx_event_outbox_status_retry_created", columnList = "status, next_retry_at, created_at")
})
public class EventOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventOutboxStatus status = EventOutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    public EventOutbox(String eventType, String aggregateType, Long aggregateId, String payload) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public void markProcessed() {
        this.status = EventOutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    public void recordFailure(int maxRetryCount, LocalDateTime nextRetryAt) {
        this.retryCount++;

        if (this.retryCount >= maxRetryCount) {
            this.status = EventOutboxStatus.FAILED;
            this.nextRetryAt = null;
            return;
        }

        this.status = EventOutboxStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
    }
}
