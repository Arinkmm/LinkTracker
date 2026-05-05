package backend.academy.linktracker.bot.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private String topic;
    private String groupId;
    private String dltTopic;
    private int maxRetries;
    private long backoffMs;
    private int maxDedupCacheSize;
    private Duration dedupTtl;
}
