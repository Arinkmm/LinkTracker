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
    public static final String API_LINKS_DELETE = "links_delete";
    public static final String API_LINKS_GET = "links_get";
    public static final String API_LINKS_POST = "links_post";
    public static final String API_TG_CHAT_DELETE = "tg_chat_delete";
    public static final String API_TG_CHAT_POST = "tg_chat_post";
    public static final String SCOPE_DATABASE = "database";
    public static final String SCOPE_EXTERNAL_SOURCE = "external_source";
    public static final String SCOPE_LLM_AGENT = "llm_agent";
    public static final String SOURCE_GITHUB = "github";
    public static final String SOURCE_OTHER = "other";
    public static final String SOURCE_STACKOVERFLOW = "stackoverflow";
    public static final String TYPE_CHATS = "chats";
    public static final String TYPE_KAFKA = "kafka";
    public static final String TYPE_LINKS = "links";
    public static final String TYPE_OUTBOX_MESSAGES = "outbox_messages";
    public static final String TYPE_SUBSCRIPTIONS = "subscriptions";

    private static final String API_REQUESTS_METRIC = "api_requests_total";
    private static final String REQUEST_DURATION_METRIC = "request_duration_ms";
    private static final String TAG_SCOPE = "scope";
    private static final String TAG_SCOPE_TYPE = "scope_type";
    private static final String TAG_SOURCE = "source";
    private static final double[] REQUEST_DURATION_BUCKETS_MS = {10, 50, 100, 250, 500, 1000, 2500, 5000, 10000};

    private final MeterRegistry meterRegistry;

    public void incrementApiRequest(String source) {
        meterRegistry.counter(API_REQUESTS_METRIC, TAG_SOURCE, source).increment();
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
            return SOURCE_OTHER;
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("github.com") || host.equals("www.github.com")) {
            return SOURCE_GITHUB;
        }
        if (host.equals("stackoverflow.com") || host.endsWith(".stackoverflow.com")) {
            return SOURCE_STACKOVERFLOW;
        }
        return SOURCE_OTHER;
    }

    public void recordRequestDuration(String scope, String scopeType, double durationMs) {
        DistributionSummary.builder(REQUEST_DURATION_METRIC)
                .serviceLevelObjectives(REQUEST_DURATION_BUCKETS_MS)
                .tag(TAG_SCOPE, scope)
                .tag(TAG_SCOPE_TYPE, scopeType)
                .register(meterRegistry)
                .record(durationMs);
    }

    private double elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
