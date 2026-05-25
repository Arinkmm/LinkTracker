package backend.academy.linktracker.scrapper.properties;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "resilience4j.retry.configs.default")
public class RetryProperties {
    private final List<Integer> retryableStatusCodes;
}
