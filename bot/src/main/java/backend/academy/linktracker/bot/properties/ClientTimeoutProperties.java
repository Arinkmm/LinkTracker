package backend.academy.linktracker.bot.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.client.timeout")
public class ClientTimeoutProperties {
    private final Duration connectTimeout;
    private final Duration readTimeout;
}
