package backend.academy.linktracker.bot.service.consumer;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.updates.type", havingValue = "kafka")
@Slf4j
@RequiredArgsConstructor
public class DltConsumer {

    @KafkaListener(
            topics = "${app.kafka.dlt-topic}",
            groupId = "${app.kafka.group-id}-dlt",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ProcessedLinkUpdateEvent linkUpdateEvent) {
        log.atError().addKeyValue("linkUpdateEvent", linkUpdateEvent).log("Message moved to DLT");
    }
}
