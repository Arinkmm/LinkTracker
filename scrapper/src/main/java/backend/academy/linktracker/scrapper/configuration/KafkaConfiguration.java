package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
public class KafkaConfiguration {
    @Bean
    public ProducerFactory<String, LinkUpdateEvent> producerFactory(
        org.springframework.boot.kafka.autoconfigure.KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, LinkUpdateEvent> kafkaTemplate(
        ProducerFactory<String, LinkUpdateEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
