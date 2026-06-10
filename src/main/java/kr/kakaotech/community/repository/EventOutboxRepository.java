package kr.kakaotech.community.repository;

import kr.kakaotech.community.entity.EventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    @Query(value = """
            SELECT *
            FROM event_outbox
            WHERE event_type = :eventType
              AND aggregate_type = :aggregateType
              AND status = :status
              AND (next_retry_at IS NULL OR next_retry_at <= :now)
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE
            """, nativeQuery = true)
    Optional<EventOutbox> findFirstProcessableForUpdate(
            @Param("eventType") String eventType,
            @Param("aggregateType") String aggregateType,
            @Param("status") String status,
            @Param("now") LocalDateTime now
    );
}
