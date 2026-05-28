package backend.academy.linktracker.ai.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private String rawTopic;
    private String rawDltTopic;
    private String processedTopic;
    private String groupId;
    private int maxRetries;
    private long backoffMs;
    private int maxDedupCacheSize;
    private Duration dedupTtl;
}
