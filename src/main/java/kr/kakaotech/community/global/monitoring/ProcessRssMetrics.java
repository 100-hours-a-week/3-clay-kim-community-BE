package kr.kakaotech.community.global.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class ProcessRssMetrics implements MeterBinder {

    private static final Path PROCESS_STATUS = Path.of("/proc/self/status");
    private static final long BYTES_PER_KILOBYTE = 1024L;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("process.memory.rss", this, ProcessRssMetrics::readRssBytes)
                .description("Resident set size of the Java process")
                .baseUnit("bytes")
                .register(registry);
    }

    double readRssBytes() {
        try {
            return parseRssBytes(Files.readAllLines(PROCESS_STATUS));
        } catch (IOException | RuntimeException exception) {
            return Double.NaN;
        }
    }

    static double parseRssBytes(List<String> statusLines) {
        return statusLines.stream()
                .filter(line -> line.startsWith("VmRSS:"))
                .findFirst()
                .map(ProcessRssMetrics::parseKilobytes)
                .orElse(Double.NaN);
    }

    private static double parseKilobytes(String vmRssLine) {
        String[] fields = vmRssLine.trim().split("\\s+");
        if (fields.length < 2) {
            return Double.NaN;
        }

        return Long.parseLong(fields[1]) * BYTES_PER_KILOBYTE;
    }
}
