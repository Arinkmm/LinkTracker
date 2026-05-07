package backend.academy.linktracker.scrapper.service.notifier.impl.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private final OutboxMessageService outboxMessageService;
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.kafka.outbox-checking-interval-ms}")
    public void runRelay() {
        List<OutboxMessageEntity> messages = outboxMessageService.getOutboxMessages(
                kafkaProperties.getMaxRetriesForMessagesOutbox(), kafkaProperties.getOutboxCheckingLimit());

        if (messages.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures =
                messages.stream().map(this::processMessage).collect(Collectors.toList());

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.atError().addKeyValue("errorMessage", e.getMessage()).log("Batch processing failed");
        }
    }

    private CompletableFuture<Void> processMessage(OutboxMessageEntity message) {
        try {
            LinkUpdate linkUpdate = objectMapper.readValue(message.getPayload(), LinkUpdate.class);

            LinkUpdateEvent linkUpdateEvent = LinkUpdateEvent.newBuilder()
                    .setId(linkUpdate.getId())
                    .setUrl(linkUpdate.getUrl().toString())
                    .setDescription(linkUpdate.getDescription())
                    .setTgChatIds(linkUpdate.getTgChatIds())
                    .build();

            log.atInfo().addKeyValue("messageId", message.getId()).log("Sending link update event");

            return kafkaTemplate
                    .send(kafkaProperties.getTopic(), String.valueOf(linkUpdateEvent.getId()), linkUpdateEvent)
                    .handle((result, ex) -> {
                        if (ex != null) {
                            log.atError()
                                    .addKeyValue("messageId", message.getId())
                                    .log("Failed to send message");
                            outboxMessageService.markAsError(message);
                        } else {
                            log.atInfo()
                                    .addKeyValue("messageId", message.getId())
                                    .log("Successfully sent message");
                            outboxMessageService.markAsSent(message);
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.atError().addKeyValue("messageId", message.getId()).log("Error serializing/processing message");
            outboxMessageService.markAsError(message);
            return CompletableFuture.completedFuture(null);
        }
    }
}
