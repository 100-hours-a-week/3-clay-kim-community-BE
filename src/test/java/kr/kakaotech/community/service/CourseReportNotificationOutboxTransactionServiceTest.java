package kr.kakaotech.community.service;

import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.EventOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CourseReportNotificationOutboxTransactionServiceTest {

    @Mock
    EventOutboxRepository eventOutboxRepository;

    @Test
    @DisplayName("claim은 처리할 이벤트를 PROCESSING으로 변경하고 lease 시작 시각을 기록한다")
    void claimNext_marksEventProcessing() {
        EventOutbox outbox = outbox(0);
        given(eventOutboxRepository.findFirstProcessableForUpdate(
                eq("COURSE_REPORT_CREATED"),
                eq("COURSE_REPORT"),
                eq(EventOutboxStatus.PENDING.name()),
                eq(EventOutboxStatus.PROCESSING.name()),
                any(),
                any()
        )).willReturn(Optional.of(outbox));

        var claim = transactionService().claimNext().orElseThrow();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSING);
        assertThat(outbox.getProcessingStartedAt()).isEqualTo(claim.processingStartedAt());
        verify(eventOutboxRepository).findFirstProcessableForUpdate(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                EventOutboxStatus.PENDING.name(),
                EventOutboxStatus.PROCESSING.name(),
                claim.processingStartedAt(),
                claim.processingStartedAt().minusMinutes(5)
        );
    }

    @Test
    @DisplayName("현재 claim의 성공만 PROCESSED로 기록한다")
    void markProcessed_onlyCurrentClaim() {
        EventOutbox outbox = outbox(0);
        LocalDateTime processingStartedAt = LocalDateTime.now();
        outbox.markProcessing(processingStartedAt);
        var claim = claim(outbox, processingStartedAt);
        given(eventOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));

        boolean processed = transactionService().markProcessed(claim);

        assertThat(processed).isTrue();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSED);
        assertThat(outbox.getProcessingStartedAt()).isNull();
    }

    @Test
    @DisplayName("처리 실패는 PROCESSING과 분리해 재시도 상태로 기록한다")
    void recordFailure_schedulesRetry() {
        EventOutbox outbox = outbox(0);
        LocalDateTime processingStartedAt = LocalDateTime.now();
        outbox.markProcessing(processingStartedAt);
        var claim = claim(outbox, processingStartedAt);
        given(eventOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));

        var failure = transactionService().recordFailure(claim).orElseThrow();

        assertThat(failure.status()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(failure.retryCount()).isEqualTo(1);
        assertThat(failure.nextRetryAt()).isNotNull();
        assertThat(outbox.getProcessingStartedAt()).isNull();
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 FAILED로 기록한다")
    void recordFailure_marksFailedAtRetryLimit() {
        EventOutbox outbox = outbox(2);
        LocalDateTime processingStartedAt = LocalDateTime.now();
        outbox.markProcessing(processingStartedAt);
        var claim = claim(outbox, processingStartedAt);
        given(eventOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));

        var failure = transactionService().recordFailure(claim).orElseThrow();

        assertThat(failure.status()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(failure.retryCount()).isEqualTo(3);
        assertThat(failure.nextRetryAt()).isNull();
    }

    @Test
    @DisplayName("lease 만료 뒤 재claim된 이벤트에는 이전 워커가 완료 상태를 기록하지 못한다")
    void markProcessed_ignoresExpiredClaim() {
        EventOutbox outbox = outbox(0);
        LocalDateTime expiredClaimStartedAt = LocalDateTime.now().minusMinutes(10);
        LocalDateTime currentClaimStartedAt = LocalDateTime.now();
        outbox.markProcessing(currentClaimStartedAt);
        given(eventOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));

        boolean processed = transactionService().markProcessed(claim(outbox, expiredClaimStartedAt));

        assertThat(processed).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSING);
        assertThat(outbox.getProcessingStartedAt()).isEqualTo(currentClaimStartedAt);
    }

    private CourseReportNotificationOutboxTransactionService transactionService() {
        return new CourseReportNotificationOutboxTransactionService(eventOutboxRepository);
    }

    private EventOutbox outbox(int retryCount) {
        EventOutbox outbox = new EventOutbox(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                99L,
                "{}"
        );
        ReflectionTestUtils.setField(outbox, "id", 1L);
        ReflectionTestUtils.setField(outbox, "retryCount", retryCount);
        return outbox;
    }

    private CourseReportNotificationOutboxTransactionService.Claim claim(
            EventOutbox outbox,
            LocalDateTime processingStartedAt
    ) {
        return new CourseReportNotificationOutboxTransactionService.Claim(
                outbox.getId(),
                outbox.getEventId(),
                outbox.getAggregateId(),
                processingStartedAt
        );
    }
}
