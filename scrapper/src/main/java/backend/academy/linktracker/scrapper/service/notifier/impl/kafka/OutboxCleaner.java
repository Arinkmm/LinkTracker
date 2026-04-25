package backend.academy.linktracker.scrapper.service.notifier.impl.kafka;

import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.repository.OutboxMessageRepository;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Slf4j
public class OutboxCleaner {
    private final OutboxMessageService outboxMessageService;
    private final KafkaProperties kafkaProperties;

    @Scheduled(fixedDelayString = "${app.kafka.days-interval-for-cleaning-outbox}")
    public void cleanOutboxMessages() {
        log.info("Start cleaning outbox messages");
        outboxMessageService.deleteOldSentMessages(kafkaProperties.getDaysIntervalForCleaningOutbox());
    }
}
