package backend.academy.linktracker.scrapper.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScrapperMetrics {
    private static final double[] REQUEST_DURATION_BUCKETS_MS = {10, 50, 100, 250, 500, 1000, 2500, 5000, 10000};

    private final MeterRegistry meterRegistry;

    public void incrementApiRequest(String source) {
        meterRegistry.counter("api_requests_total", "source", source).increment();
    }

    public <T> T recordRequestDuration(String scope, String scopeType, Supplier<T> action) {
        long start = System.nanoTime();
        try {
            return action.get();
        } finally {
            recordRequestDuration(scope, scopeType, elapsedMillis(start));
        }
    }

    public void recordRequestDuration(String scope, String scopeType, Runnable action) {
        long start = System.nanoTime();
        try {
            action.run();
        } finally {
            recordRequestDuration(scope, scopeType, elapsedMillis(start));
        }
    }

    public String trackedSource(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return "other";
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("github.com") || host.equals("www.github.com")) {
            return "github";
        }
        if (host.equals("stackoverflow.com") || host.endsWith(".stackoverflow.com")) {
            return "stackoverflow";
        }
        return "other";
    }

    public void recordRequestDuration(String scope, String scopeType, double durationMs) {
        DistributionSummary.builder("request_duration_ms")
                .serviceLevelObjectives(REQUEST_DURATION_BUCKETS_MS)
                .tag("scope", scope)
                .tag("scope_type", scopeType)
                .register(meterRegistry)
                .record(durationMs);
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
