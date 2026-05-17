package backend.academy.linktracker.bot.properties;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.resilience")
public class ResilienceProperties {
    private final Timeout timeout;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    @Getter
    @RequiredArgsConstructor
    public static class Timeout {
        private final Duration connectTimeout;
        private final Duration readTimeout;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Retry {
        private final int maxAttempts;
        private final Duration waitDuration;
        private final String backoffStrategy;
        private final double exponentialMultiplier;
        private final List<Integer> retryableStatusCodes;
    }

    @Getter
    @RequiredArgsConstructor
    public static class CircuitBreaker {
        private final int slidingWindowSize;
        private final int minimumNumberOfCalls;
        private final float failureRateThreshold;
        private final int permittedCallsInHalfOpenState;
        private final Duration waitDurationInOpenState;
    }
}
