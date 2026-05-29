package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.ai.kafka.producer.ProcessedUpdateProducer;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class UpdateGroupPublisher {
    private final UpdateGroupAggregator aggregator;
    private final ProcessedUpdateProducer producer;

    void publish(UpdateGroup group) {
        ProcessedLinkUpdateEvent event = aggregator.aggregate(group);
        log.atInfo()
                .addKeyValue("id", event.getId())
                .addKeyValue("priority", event.getPriority())
                .addKeyValue("group_size", group.size())
                .log("Publishing grouped update");
        producer.publish(event);
    }
}
