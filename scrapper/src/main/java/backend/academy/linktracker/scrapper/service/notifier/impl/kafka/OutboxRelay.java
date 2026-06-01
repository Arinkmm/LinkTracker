package backend.academy.linktracker.scrapper.service.notifier.impl.kafka;

import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationProperties notificationProperties;
    private final ScrapperMetrics metrics;

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
            log.atError()
                    .setCause(e)
                    .addKeyValue("errorMessage", e.getMessage())
                    .log("Batch processing failed");
        }
    }

    private CompletableFuture<Void> processMessage(OutboxMessageEntity message) {
        try {
            LinkUpdate linkUpdate = objectMapper.readValue(message.getPayload(), LinkUpdate.class);

            RawLinkUpdateEvent linkUpdateEvent = RawLinkUpdateEvent.newBuilder()
                    .setId(linkUpdate.getId())
                    .setUrl(linkUpdate.getUrl().toString())
                    .setDescription(linkUpdate.getDescription())
                    .setAuthor(extractAuthor(linkUpdate.getDescription()))
                    .setTgChatIds(linkUpdate.getTgChatIds())
                    .build();

            log.atInfo().addKeyValue("messageId", message.getId()).log("Sending link update event");

            long start = System.nanoTime();
            return kafkaTemplate
                    .send(kafkaProperties.getTopic(), String.valueOf(linkUpdateEvent.getId()), linkUpdateEvent)
                    .handle((result, ex) -> {
                        metrics.recordRequestDuration(
                                ScrapperMetrics.SCOPE_LLM_AGENT,
                                ScrapperMetrics.TYPE_KAFKA,
                                (System.nanoTime() - start) / 1_000_000.0);
                        if (ex != null) {
                            log.atError()
                                    .setCause(ex)
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
            log.atError()
                    .setCause(e)
                    .addKeyValue("messageId", message.getId())
                    .log("Error serializing/processing message");
            outboxMessageService.markAsError(message);
            return CompletableFuture.completedFuture(null);
        }
    }

    private String extractAuthor(String description) {
        if (description == null || description.isBlank()) {
            return "unknown";
        }
        String authorLabel = notificationProperties.getLabels().getAuthor();
        return description
                .lines()
                .map(String::trim)
                .filter(line -> line.startsWith(authorLabel))
                .map(line -> line.substring(authorLabel.length()).trim())
                .filter(author -> !author.isBlank())
                .findFirst()
                .orElse("unknown");
    }
}
