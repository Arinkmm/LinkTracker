package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.properties.KafkaProperties;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfiguration {
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaOperations<Object, Object> kafkaTemplate, KafkaProperties kafkaProperties) {
        return new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, ex) -> new TopicPartition(kafkaProperties.getDltTopic(), record.partition()));
    }

    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer, KafkaProperties kafkaProperties) {
        FixedBackOff backOff = new FixedBackOff(kafkaProperties.getBackoffMs(), kafkaProperties.getAttempts());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        errorHandler.addNotRetryableExceptions(
                DeserializationException.class, MessageConversionException.class, IllegalArgumentException.class);

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LinkUpdateEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, LinkUpdateEvent> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, LinkUpdateEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
