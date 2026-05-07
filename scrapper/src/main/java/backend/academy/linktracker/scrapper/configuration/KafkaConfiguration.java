package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ConditionalOnProperty(name = "app.updates.type", havingValue = "kafka")
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
