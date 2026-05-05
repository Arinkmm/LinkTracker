package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private String topic;
    private int outboxCheckingIntervalMs;
    private int outboxCheckingLimit;
    private int maxRetriesForMessagesOutbox;
    private int daysIntervalForCleaningOutbox;
}
