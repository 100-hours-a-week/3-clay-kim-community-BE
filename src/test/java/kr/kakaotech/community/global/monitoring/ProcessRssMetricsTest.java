package kr.kakaotech.community.global.monitoring;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessRssMetricsTest {

    @Test
    void VmRSS를_바이트로_변환한다() {
        double rssBytes = ProcessRssMetrics.parseRssBytes(List.of(
                "Name:\tjava",
                "VmSize:\t 123456 kB",
                "VmRSS:\t   1024 kB"
        ));

        assertThat(rssBytes).isEqualTo(1024D * 1024D);
    }

    @Test
    void VmRSS가_없으면_NaN을_반환한다() {
        double rssBytes = ProcessRssMetrics.parseRssBytes(List.of("Name:\tjava"));

        assertThat(rssBytes).isNaN();
    }
}
