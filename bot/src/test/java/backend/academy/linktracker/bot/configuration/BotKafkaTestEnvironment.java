package backend.academy.linktracker.bot.configuration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class BotKafkaTestEnvironment {
    static final String KAFKA_IMAGE = "confluentinc/cp-kafka:8.2.0";

    @Container
    protected static final KafkaContainer kafka = createKafkaContainer();

    private static KafkaContainer createKafkaContainer() {
        return new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE)).withKraft();
    }

    @RegisterExtension
    protected static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    protected static void registerKafkaProperties(
            DynamicPropertyRegistry registry, String mockRegistry, String topic, String dltTopic, String groupId) {

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add(
                "spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> mockRegistry);
        registry.add("spring.kafka.consumer.properties.specific.avro.reader", () -> "true");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer", () -> "io.confluent.kafka.serializers.KafkaAvroSerializer");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> mockRegistry);

        registry.add("app.updates.type", () -> "kafka");
        registry.add("app.kafka.topic", () -> topic);
        registry.add("app.kafka.dlt-topic", () -> dltTopic);
        registry.add("app.kafka.group-id", () -> groupId);
    }

    protected static void registerTelegramProperties(DynamicPropertyRegistry registry) {
        registry.add("app.telegram.token", () -> "test-token");
        registry.add("app.telegram.url", () -> wireMock.baseUrl() + "/bot");
    }

    protected <V> void publish(String topic, String mockRegistry, String key, V value) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, mockRegistry);

        try (KafkaProducer<String, V> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        }
    }
}
