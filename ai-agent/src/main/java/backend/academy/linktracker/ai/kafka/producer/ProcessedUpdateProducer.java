package backend.academy.linktracker.ai.kafka.producer;

import backend.academy.linktracker.ai.properties.KafkaProperties;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessedUpdateProducer {
    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(ProcessedLinkUpdateEvent event) {
        kafkaTemplate
                .send(kafkaProperties.getProcessedTopic(), String.valueOf(event.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.atError()
                                .setCause(ex)
                                .addKeyValue("id", event.getId())
                                .addKeyValue("topic", kafkaProperties.getProcessedTopic())
                                .log("Failed to publish processed update");
                    } else {
                        log.atInfo()
                                .addKeyValue("id", event.getId())
                                .addKeyValue("topic", kafkaProperties.getProcessedTopic())
                                .log("Processed update published");
                    }
                })
                .join();
    }
}
