package backend.academy.linktracker.ai.kafka.consumer;

import backend.academy.linktracker.ai.kafka.producer.ProcessedUpdateProducer;
import backend.academy.linktracker.ai.processing.UpdateProcessingService;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawUpdateConsumer {
    private final UpdateProcessingService processingService;
    private final ProcessedUpdateProducer processedUpdateProducer;
    private final Cache<String, Boolean> dedupCache;

    @KafkaListener(
            topics = "${app.kafka.raw-topic}",
            groupId = "${app.kafka.group-id}",
            containerFactory = "rawUpdateKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, RawLinkUpdateEvent> record) {
        String key = record.topic() + "-" + record.partition() + "-" + record.offset();
        if (dedupCache.getIfPresent(key) != null) {
            log.atWarn().addKeyValue("key", key).log("Duplicate raw update, skipping");
            return;
        }
        dedupCache.put(key, Boolean.TRUE);

        RawLinkUpdateEvent event = record.value();
        log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("author", event.getAuthor())
                .addKeyValue("offset", record.offset())
                .log("Raw update received");

        processingService.process(event).ifPresentOrElse(processedUpdateProducer::publish, () -> log.atInfo()
                .addKeyValue("id", event.getId())
                .log("Raw update filtered out"));
    }
}
