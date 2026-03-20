package kr.kakaotech.community.global.monitoring;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class QueryCountMetrics {

    private final MeterRegistry meterRegistry;
    private final DistributionSummary globalSummary;

    public QueryCountMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.globalSummary = DistributionSummary.builder("jpa.query.count")
                .description("Number of SQL queries per API request")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void record(int queryCount, String method, String uri) {
        globalSummary.record(queryCount);
        DistributionSummary.builder("jpa.query.count.by.endpoint")
                .description("Number of SQL queries per API endpoint")
                .tag("method", method)
                .tag("uri", uri)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record(queryCount);
    }
}
