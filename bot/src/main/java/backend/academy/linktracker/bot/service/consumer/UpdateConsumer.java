package backend.academy.linktracker.bot.service.consumer;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.properties.KafkaProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PostConstruct;
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
    private final KafkaProperties kafkaProperties;

    private Cache<String, Boolean> processedOffsets;

    @PostConstruct
    public void init() {
        this.processedOffsets = CacheBuilder.newBuilder()
                .maximumSize(kafkaProperties.getMaxDedupCacheSize())
                .expireAfterWrite(kafkaProperties.getDedupTtl())
                .build();
    }

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, LinkUpdateEvent> record) {
        String key = record.topic() + "-" + record.partition() + "-" + record.offset();
        if (processedOffsets.getIfPresent(key) != null) {
            log.atWarn().addKeyValue("key", key).log("Duplicate message, skipping");
            return;
        }
        processedOffsets.put(key, Boolean.TRUE);

        LinkUpdateEvent linkUpdateEvent = record.value();
        validate(linkUpdateEvent);

        log.atInfo()
                .addKeyValue("linkUpdateEvent", linkUpdateEvent)
                .addKeyValue("offset", record.offset())
                .log("Message received");

        linkUpdateEvent
                .getTgChatIds()
                .forEach(chatId -> telegramSender.sendMessage(chatId, linkUpdateEvent.getDescription()));
    }

    private void validate(LinkUpdateEvent linkUpdateEvent) {
        if (linkUpdateEvent.getId() == 0L) {
            throw new IllegalArgumentException("link id is required");
        }
        if (linkUpdateEvent.getDescription() == null
                || linkUpdateEvent.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (linkUpdateEvent.getTgChatIds() == null
                || linkUpdateEvent.getTgChatIds().isEmpty()) {
            throw new IllegalArgumentException("tgChatIds must not be empty");
        }
    }
}
