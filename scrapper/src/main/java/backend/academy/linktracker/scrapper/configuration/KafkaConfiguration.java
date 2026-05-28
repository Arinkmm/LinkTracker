package backend.academy.linktracker.scrapper.configuration;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;

@Configuration
public class KafkaConfiguration {
    @Bean
    public ProducerFactory<String, Object> producerFactory(
            org.springframework.boot.kafka.autoconfigure.KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = new LinkedHashMap<>(kafkaProperties.buildProducerProperties());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(SpecificRecord.class, new KafkaAvroSerializer());

        return new DefaultKafkaProducerFactory<>(
                producerProperties, new StringSerializer(), new DelegatingByTypeSerializer(delegates, true));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
