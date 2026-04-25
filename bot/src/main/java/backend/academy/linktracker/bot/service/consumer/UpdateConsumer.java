package backend.academy.linktracker.bot.service.consumer;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.updates.type", havingValue = "kafka")
@Slf4j
@RequiredArgsConstructor
public class UpdateConsumer {
    private final TelegramSender telegramSender;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(LinkUpdateEvent linkUpdateEvent) {
        validate(linkUpdateEvent);
        log.atInfo().addKeyValue("linkUpdateEvent", linkUpdateEvent).log("Message received");
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
