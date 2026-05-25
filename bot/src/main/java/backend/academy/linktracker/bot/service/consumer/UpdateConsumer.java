package backend.academy.linktracker.bot.service.consumer;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.updates.type", havingValue = "kafka")
@Slf4j
@RequiredArgsConstructor
public class UpdateConsumer {
    private final TelegramSender telegramSender;

    private final Cache<String, Boolean> dedupCache;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, ProcessedLinkUpdateEvent> record) {
        String key = record.topic() + "-" + record.partition() + "-" + record.offset();
        if (dedupCache.getIfPresent(key) != null) {
            log.atWarn().addKeyValue("key", key).log("Duplicate message, skipping");
            return;
        }
        dedupCache.put(key, Boolean.TRUE);

        ProcessedLinkUpdateEvent event = record.value();
        validate(event);

        log.atInfo()
                .addKeyValue("linkUpdateEvent", event)
                .addKeyValue("offset", record.offset())
                .addKeyValue("priority", event.getPriority())
                .log("Message received");

        event.getTgChatIds().forEach(chatId -> telegramSender.sendMessage(chatId, event.getDescription()));
    }

    private void validate(ProcessedLinkUpdateEvent event) {
        if (event.getId() == 0L) {
            throw new IllegalArgumentException("link id is required");
        }
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (event.getTgChatIds() == null || event.getTgChatIds().isEmpty()) {
            throw new IllegalArgumentException("tgChatIds must not be empty");
        }
    }
}
