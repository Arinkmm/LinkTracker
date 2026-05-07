package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.SyncBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.KafkaBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.OutboxCleaner;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.OutboxRelay;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class NotifierConfiguration {
    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http")
    public BotNotifier httpNotifier(BotClient httpBotClient) {
        return new SyncBotNotifier(httpBotClient);
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotNotifier grpcNotifier(BotClient grpcBotClient) {
        return new SyncBotNotifier(grpcBotClient);
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "kafka")
    public BotNotifier kafkaNotifier(ObjectMapper objectMapper, OutboxMessageService outboxMessageService) {
        return new KafkaBotNotifier(objectMapper, outboxMessageService);
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "kafka")
    public OutboxRelay outboxRelay(
            OutboxMessageService outboxMessageService,
            KafkaProperties kafkaProperties,
            KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new OutboxRelay(outboxMessageService, kafkaProperties, kafkaTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "kafka")
    public OutboxCleaner outboxCleaner(OutboxMessageService outboxMessageService, KafkaProperties kafkaProperties) {
        return new OutboxCleaner(outboxMessageService, kafkaProperties);
    }
}
