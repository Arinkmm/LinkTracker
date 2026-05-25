package backend.academy.linktracker.ai.configuration;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class AiAgentKafkaTestEnvironment {
    protected static final String MOCK_SCHEMA_REGISTRY = "mock://ai-agent-test-scope";
    protected static final String RAW_TOPIC = "link.raw-updates." + UUID.randomUUID();
    protected static final String RAW_DLT_TOPIC = RAW_TOPIC + ".DLT";
    protected static final String PROCESSED_TOPIC = "link.processed-updates." + UUID.randomUUID();
    protected static final String GROUP_ID = "ai-agent-test-group-" + UUID.randomUUID();

    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:8.2.0";

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE)).withKraft();

    protected static final WireMockServer wireMock =
            new WireMockServer(wireMockConfig().dynamicPort());

    static {
        wireMock.start();
    }

    @AfterEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @DynamicPropertySource
    static void registerAiAgentProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add(
                "spring.kafka.consumer.key-deserializer",
                () -> "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        registry.add(
                "spring.kafka.consumer.properties.spring.deserializer.key.delegate.class",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add(
                "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class",
                () -> "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> MOCK_SCHEMA_REGISTRY);
        registry.add("spring.kafka.consumer.properties.specific.avro.reader", () -> "true");

        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer", () -> "io.confluent.kafka.serializers.KafkaAvroSerializer");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> MOCK_SCHEMA_REGISTRY);
        registry.add("spring.kafka.producer.properties.auto.register.schemas", () -> "true");

        registry.add("app.kafka.raw-topic", () -> RAW_TOPIC);
        registry.add("app.kafka.raw-dlt-topic", () -> RAW_DLT_TOPIC);
        registry.add("app.kafka.processed-topic", () -> PROCESSED_TOPIC);
        registry.add("app.kafka.group-id", () -> GROUP_ID);
        registry.add("app.kafka.max-retries", () -> "0");
        registry.add("app.kafka.backoff-ms", () -> "0");
        registry.add("app.kafka.max-dedup-cache-size", () -> "1000");
        registry.add("app.kafka.dedup-ttl", () -> "10m");

        registry.add("ai-agent.labels.description", () -> "Описание: ");
        registry.add("ai-agent.filtering.stop-words[0]", () -> "spam");
        registry.add("ai-agent.filtering.excluded-authors[0]", () -> "bot-user");
        registry.add("ai-agent.filtering.min-length", () -> "5");
        registry.add("ai-agent.summarization.threshold", () -> "30");
        registry.add("ai-agent.summarization.api.url", () -> wireMock.baseUrl() + "/v1/chat/completions");
        registry.add("ai-agent.summarization.api.token", () -> "test-token");
        registry.add("ai-agent.summarization.api.model", () -> "test-model");
        registry.add("ai-agent.summarization.api.timeout", () -> "2s");
        registry.add("ai-agent.summarization.api.prompt", () -> "Summarize");
    }

    protected <V> void publishAvro(String topic, String key, V value) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);

        try (KafkaProducer<String, V> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        }
    }

    protected void publishInvalid(String topic, String key) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, "not-avro".getBytes(StandardCharsets.UTF_8)))
                    .get();
        }
    }

    protected void awaitRawDltMessage(String key, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "raw-dlt-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(RAW_DLT_TOPIC));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, byte[]> record : records) {
                    if (key.equals(record.key())) {
                        return;
                    }
                }
            }
        }
        throw new AssertionError("Raw update was not published to DLT for key: " + key);
    }

    protected ProcessedLinkUpdateEvent consumeProcessed(String key, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "processed-verifier-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        try (KafkaConsumer<String, ProcessedLinkUpdateEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(PROCESSED_TOPIC));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, ProcessedLinkUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, ProcessedLinkUpdateEvent> record : records) {
                    if (key.equals(record.key())) {
                        return record.value();
                    }
                }
            }
        }
        throw new AssertionError("Processed update was not published for key: " + key);
    }
}
