package backend.academy.linktracker.bot.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateConsumerKafkaIntegrationTest {
    private static final String MOCK_REGISTRY = "mock://bot-test-scope";
    private static final String TOPIC = "link-updates-bot-test";
    private static final String DLT_TOPIC = TOPIC + ".DLT";
    private static final String GROUP_ID = "bot-test-group";

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @RegisterExtension
    static final WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @MockitoBean
    TelegramSender telegramSender;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.key-deserializer",
            () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
            () -> "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () -> MOCK_REGISTRY);
        registry.add("spring.kafka.consumer.properties.specific.avro.reader", () -> "true");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("spring.kafka.producer.key-serializer",
            () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
            () -> "io.confluent.kafka.serializers.KafkaAvroSerializer");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () -> MOCK_REGISTRY);

        registry.add("app.updates.type",() -> "kafka");
        registry.add("app.kafka.topic",() -> TOPIC);
        registry.add("app.kafka.dlt-topic",() -> DLT_TOPIC);
        registry.add("app.kafka.group-id",() -> GROUP_ID);

        registry.add("app.telegram.token",() -> "test-token");
        registry.add("app.telegram.url",() -> wireMock.baseUrl() + "/bot");
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

        wireMock.stubFor(post(urlPathMatching("/bot.*/sendMessage"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                                {"ok":true,"result":{"message_id":1,"chat":{"id":1},"text":"ok"}}
                                """)));
    }

    @Test
    @DisplayName("Consumer вызывает TelegramSender.sendMessage для каждого chatId из события")
    void shouldCallTelegramSenderForEachChatId() throws Exception {
        long linkId = 42L;
        String description = "New GitHub issue: Bug report";
        List<Long> chatIds = List.of(100L, 200L, 300L);

        LinkUpdateEvent event = LinkUpdateEvent.newBuilder()
            .setId(linkId)
            .setUrl("https://github.com/user/repo")
            .setDescription(description)
            .setTgChatIds(chatIds)
            .build();

        publish(event);

        chatIds.forEach(chatId ->
            verify(telegramSender, timeout(10_000))
                .sendMessage(chatId, description));
    }

    @Test
    @DisplayName("Consumer вызывает sendMessage ровно один раз на chatId (не дублирует)")
    void shouldSendTelegramMessageExactlyOnce() throws Exception {
        LinkUpdateEvent event = LinkUpdateEvent.newBuilder()
            .setId(99L)
            .setUrl("https://github.com/user/repo2")
            .setDescription("Duplicate delivery simulation")
            .setTgChatIds(List.of(55L))
            .build();

        publish(event);

        verify(telegramSender, timeout(10_000).times(1))
            .sendMessage(55L, "Duplicate delivery simulation");

        verify(telegramSender, after(1_500).times(1))
            .sendMessage(55L, "Duplicate delivery simulation");
    }


    private void publish(LinkUpdateEvent event) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);

        try (KafkaProducer<String, LinkUpdateEvent> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(TOPIC, String.valueOf(event.getId()), event)).get();
        }
    }
}
