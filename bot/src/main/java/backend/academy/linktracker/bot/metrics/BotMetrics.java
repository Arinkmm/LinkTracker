package backend.academy.linktracker.bot.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotMetrics {
    public static final String REQUEST_TYPE_COMMAND = "command";
    public static final String REQUEST_TYPE_GRPC_UPDATE = "grpc_update";
    public static final String REQUEST_TYPE_HTTP_UPDATE = "http_update";
    public static final String REQUEST_TYPE_SEND_MESSAGE = "send_message";
    public static final String REQUEST_TYPE_TELEGRAM_UPDATE = "telegram_update";
    public static final String SCOPE_SCRAPPER_SYNC_API = "scrapper_sync_api";

    private static final String COMMAND_REQUESTS_METRIC = "command_requests_total";
    private static final String COMMAND_DURATION_METRIC = "command_duration_ms";
    private static final String SENT_NOTIFICATION_METRIC = "sent_notification_total";
    private static final String TELEGRAM_REQUESTS_METRIC = "telegram_requests_total";
    private static final String TAG_COMMAND = "command";
    private static final String TAG_REQUEST_TYPE = "request_type";
    private static final String TAG_SCOPE = "scope";
    private static final String TAG_SCOPE_TYPE = "scope_type";
    private static final double[] COMMAND_DURATION_BUCKETS_MS = {10, 50, 100, 250, 500, 1000, 2000, 5000};

    private final MeterRegistry meterRegistry;

    public void incrementCommandRequest(String command) {
        meterRegistry
                .counter(COMMAND_REQUESTS_METRIC, TAG_COMMAND, command, TAG_REQUEST_TYPE, REQUEST_TYPE_COMMAND)
                .increment();
    }

    public void incrementTelegramRequest(String requestType) {
        meterRegistry
                .counter(TELEGRAM_REQUESTS_METRIC, TAG_REQUEST_TYPE, requestType)
                .increment();
    }

    public void incrementSentNotification() {
        meterRegistry.counter(SENT_NOTIFICATION_METRIC).increment();
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
        DistributionSummary.builder(COMMAND_DURATION_METRIC)
                .serviceLevelObjectives(COMMAND_DURATION_BUCKETS_MS)
                .tag(TAG_COMMAND, command)
                .tag(TAG_SCOPE, scope)
                .tag(TAG_SCOPE_TYPE, scopeType)
                .register(meterRegistry)
                .record(durationMs);
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
