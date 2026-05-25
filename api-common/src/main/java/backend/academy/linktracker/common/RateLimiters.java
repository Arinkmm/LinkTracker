package backend.academy.linktracker.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import java.time.Duration;

public class RateLimiters {
    private final int capacity;
    private final Duration refillPeriod;
    private final String namePrefix;
    private final Cache<String, RateLimiter> limiters;

    public RateLimiters(int capacity, Duration refillPeriod, int maxEntries, String namePrefix) {
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
        this.namePrefix = namePrefix;
        this.limiters = Caffeine.newBuilder().maximumSize(maxEntries).build();
    }

    public boolean acquire(String key) {
        return limiters.get(key, this::createRateLimiter).acquirePermission();
    }

    private RateLimiter createRateLimiter(String key) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(capacity)
                .limitRefreshPeriod(refillPeriod)
                .timeoutDuration(Duration.ZERO)
                .build();
        return RateLimiter.of(namePrefix + "-" + key, config);
    }
}
