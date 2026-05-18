package backend.academy.linktracker.scrapper.configuration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class KafkaIntegrationEnvironment extends CacheIntegrationEnvironment {
    protected static final String MOCK_SCHEMA_REGISTRY = "mock://test-scope";
    protected static final String TEST_TOPIC = "link-updates-test";
    protected static final String TEST_DLT_TOPIC = "link-updates-test.DLT";
    protected static final String TEST_GROUP_ID = "scrapper-test-group";

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", SharedKafkaContainer.INSTANCE::getBootstrapServers);

        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer", () -> "io.confluent.kafka.serializers.KafkaAvroSerializer");
        registry.add(
                "spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "io.confluent.kafka.serializers.KafkaAvroDeserializer");

        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> MOCK_SCHEMA_REGISTRY);
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> MOCK_SCHEMA_REGISTRY);
        registry.add("spring.kafka.consumer.properties.specific.avro.reader", () -> "true");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("app.updates.type", () -> "kafka");
        registry.add("app.kafka.topic", () -> TEST_TOPIC);
        registry.add("app.kafka.dlt-topic", () -> TEST_DLT_TOPIC);
        registry.add("app.kafka.group-id", () -> TEST_GROUP_ID);
        registry.add("app.kafka.outbox-checking-interval", () -> "500");
        registry.add("app.kafka.max-retries-for-messages-outbox", () -> "3");
        registry.add("app.kafka.outbox-checking-limit", () -> "100");
        registry.add("app.kafka.days-interval-for-cleaning-outbox", () -> "7");
    }
}
