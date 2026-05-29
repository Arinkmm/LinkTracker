package backend.academy.linktracker.bot.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotMetrics {
    private static final double[] COMMAND_DURATION_BUCKETS_MS = {10, 50, 100, 250, 500, 1000, 2000, 5000};

    private final MeterRegistry meterRegistry;

    public void incrementCommandRequest(String command) {
        meterRegistry
                .counter("command_requests_total", "command", command, "request_type", "command")
                .increment();
    }

    public void incrementTelegramRequest(String requestType) {
        meterRegistry
                .counter("telegram_requests_total", "request_type", requestType)
                .increment();
    }

    public void incrementSentNotification(double amount) {
        if (amount > 0) {
            meterRegistry.counter("sent_notification_total").increment(amount);
        }
    }

    public void recordCommandDuration(String command, String scope, String scopeType, Runnable action) {
        long start = System.nanoTime();
        try {
            action.run();
        } finally {
            recordCommandDuration(command, scope, scopeType, elapsedMillis(start));
        }
    }

    private void recordCommandDuration(String command, String scope, String scopeType, double durationMs) {
        DistributionSummary.builder("command_duration_ms")
                .serviceLevelObjectives(COMMAND_DURATION_BUCKETS_MS)
                .tag("command", command)
                .tag("scope", scope)
                .tag("scope_type", scopeType)
                .register(meterRegistry)
                .record(durationMs);
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
