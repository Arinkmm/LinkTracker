package backend.academy.linktracker.scrapper.service.notifier.impl.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private final OutboxMessageService outboxMessageService;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-checking-interval}")
    public void runRelay() {
        List<OutboxMessageEntity> messages = outboxMessageService.getOutboxMessages(
            kafkaProperties.getMaxRetriesForMessagesOutbox(),
            kafkaProperties.getOutboxCheckingLimit()
        );

        for (OutboxMessageEntity message : messages) {
            try {
                LinkUpdate linkUpdate = objectMapper.readValue(message.getPayload(), LinkUpdate.class);

                LinkUpdateEvent linkUpdateEvent = LinkUpdateEvent.newBuilder()
                    .setId(linkUpdate.getId())
                    .setUrl(linkUpdate.getUrl().toString())
                    .setDescription(linkUpdate.getDescription())
                    .setTgChatIds(linkUpdate.getTgChatIds())
                    .build();

                log.atInfo().addKeyValue("id", message.getId())
                    .addKeyValue("payload", message.getPayload())
                            .log("Sending link update event");
                kafkaTemplate.send(kafkaProperties.getTopic(), String.valueOf(linkUpdateEvent.getId()), linkUpdateEvent).get();
                outboxMessageService.markAsSent(message);
            } catch (Exception e) {
                log.atError().addKeyValue("id", message.getId()).addKeyValue("payload", message.getPayload()).log("Error while sending link update event");
                outboxMessageService.markAsError(message);
            }
        }
    }
}
