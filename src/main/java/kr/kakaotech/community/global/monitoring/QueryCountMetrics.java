package kr.kakaotech.community.global.monitoring;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class QueryCountMetrics {

    private final DistributionSummary summary;

    public QueryCountMetrics(MeterRegistry meterRegistry) {
        this.summary = DistributionSummary.builder("jpa.query.count")
                .description("Number of SQL queries per API request")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void record(int queryCount) {
        summary.record(queryCount);
    }
}
