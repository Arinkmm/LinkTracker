package backend.academy.linktracker.bot.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {
    private final int capacity;
    private final Duration refillPeriod;
    private final int maxEntries;
}
