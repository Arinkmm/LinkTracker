package backend.academy.linktracker.bot.kafka;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class UpdateConsumerInvalidEventTest {
    private static final String MOCK_REGISTRY = "mock://bot-invalid-test-scope";
    private static final String TOPIC = "link-updates-invalid-test";
    private static final String DLT_TOPIC = TOPIC + ".DLT";
    private static final String GROUP_ID = "bot-invalid-test-group";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @MockitoBean
    TelegramSender telegramSender;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add(
                "spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add(
                "spring.kafka.consumer.value-deserializer",
                () -> "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> MOCK_REGISTRY);
        registry.add("spring.kafka.consumer.properties.specific.avro.reader", () -> "true");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add(
                "spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add(
                "spring.kafka.producer.value-serializer", () -> "io.confluent.kafka.serializers.KafkaAvroSerializer");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> MOCK_REGISTRY);

        registry.add("app.updates.type", () -> "kafka");
        registry.add("app.kafka.topic", () -> TOPIC);
        registry.add("app.kafka.dlt-topic", () -> DLT_TOPIC);
        registry.add("app.kafka.group-id", () -> GROUP_ID);

        registry.add("app.telegram.token", () -> "test-token");
        registry.add("app.telegram.url", () -> wireMock.baseUrl() + "/bot");
    }

    @BeforeEach
    void stubTelegramApi() {
        wireMock.stubFor(post(urlPathMatching("/bot.*/setMyCommands"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"ok":true,"result":true}
                                """)));
    }

    @Test
    @DisplayName("Невалидное событие (id=0, пустые чаты) не вызывает Telegram — уходит в DLT")
    void shouldNotCallTelegramOnInvalidEvent() throws Exception {
        LinkUpdateEvent invalidEvent = LinkUpdateEvent.newBuilder()
                .setId(0L)
                .setUrl("https://github.com")
                .setDescription("Some update")
                .setTgChatIds(List.of())
                .build();

        publish(invalidEvent);

        Awaitility.await()
                .pollDelay(Duration.ofMillis(500))
                .atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> verify(telegramSender, never()).sendMessage(anyLong(), anyString()));
    }

    private void publish(LinkUpdateEvent event) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);

        try (KafkaProducer<String, LinkUpdateEvent> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, String.valueOf(event.getId()), event))
                    .get();
        }
    }
}
