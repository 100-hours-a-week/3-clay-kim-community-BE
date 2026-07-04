package kr.kakaotech.community.integration;

import kr.kakaotech.community.entity.EventOutbox;
import kr.kakaotech.community.entity.EventOutboxStatus;
import kr.kakaotech.community.repository.EventOutboxRepository;
import kr.kakaotech.community.service.CourseReportNotificationOutboxTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.expirationtime.accessTtl=1800",
        "jwt.expirationtime.refreshTtl=604800",
        "jwt.secret=test-only-no-sensitive-secret-for-integration-test"
})
@ActiveProfiles("test")
class CourseReportNotificationOutboxTransactionIntegrationTest {

    @Autowired
    CourseReportNotificationOutboxTransactionService transactionService;
    @Autowired
    EventOutboxRepository eventOutboxRepository;

    @BeforeEach
    void clearOutbox() {
        eventOutboxRepository.deleteAll();
    }

    @Test
    @DisplayName("claim과 실패 기록은 각각 독립된 트랜잭션으로 커밋된다")
    void claimAndFailure_areCommittedIndependently() {
        EventOutbox outbox = eventOutboxRepository.saveAndFlush(new EventOutbox(
                "COURSE_REPORT_CREATED",
                "COURSE_REPORT",
                99L,
                "{}"
        ));

        var claim = transactionService.claimNext().orElseThrow();

        EventOutbox claimed = eventOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(EventOutboxStatus.PROCESSING);
        assertThat(claimed.getProcessingStartedAt()).isEqualTo(claim.processingStartedAt());

        transactionService.recordFailure(claim).orElseThrow();

        EventOutbox failed = eventOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getNextRetryAt()).isNotNull();
        assertThat(failed.getProcessingStartedAt()).isNull();
    }
}
