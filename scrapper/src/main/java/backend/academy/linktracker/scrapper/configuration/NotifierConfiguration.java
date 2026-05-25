package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.FallbackBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.SyncBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.KafkaBotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.OutboxCleaner;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.OutboxRelay;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class NotifierConfiguration {
    @Bean
    @Primary
    @ConditionalOnExpression("'${app.client.type}'=='http' or '${app.client.type}'=='grpc'")
    public BotNotifier syncNotifier(BotClient botClient, KafkaBotNotifier kafkaFallback) {
        return new FallbackBotNotifier(new SyncBotNotifier(botClient), kafkaFallback);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.client.type", havingValue = "kafka")
    public BotNotifier kafkaNotifier(KafkaBotNotifier kafkaFallback) {
        return kafkaFallback;
    }

    @Bean
    @ConditionalOnExpression(
            "'${app.client.type}'=='http' " + "or '${app.client.type}'=='grpc' " + "or '${app.client.type}'=='kafka'")
    public KafkaBotNotifier kafkaFallback(ObjectMapper objectMapper, OutboxMessageService outboxMessageService) {
        return new KafkaBotNotifier(objectMapper, outboxMessageService);
    }

    @Bean
    @ConditionalOnExpression(
            "'${app.client.type}'=='http' " + "or '${app.client.type}'=='grpc' " + "or '${app.client.type}'=='kafka'")
    public OutboxRelay outboxRelay(
            OutboxMessageService outboxMessageService,
            KafkaProperties kafkaProperties,
            KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new OutboxRelay(outboxMessageService, kafkaProperties, kafkaTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnExpression(
            "'${app.client.type}'=='http' " + "or '${app.client.type}'=='grpc' " + "or '${app.client.type}'=='kafka'")
    public OutboxCleaner outboxCleaner(OutboxMessageService outboxMessageService, KafkaProperties kafkaProperties) {
        return new OutboxCleaner(outboxMessageService, kafkaProperties);
    }
}
