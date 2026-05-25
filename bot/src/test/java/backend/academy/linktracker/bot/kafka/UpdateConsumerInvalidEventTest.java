package backend.academy.linktracker.bot.kafka;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.bot.configuration.BotKafkaTestEnvironment;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UpdateConsumerInvalidEventTest extends BotKafkaTestEnvironment {
    private static final String MOCK_REGISTRY = "mock://bot-invalid-test-scope";
    private static final String TOPIC = "link.processed-updates-invalid-test";
    private static final String DLT_TOPIC = TOPIC + ".DLT";
    private static final String GROUP_ID = "bot-invalid-test-group";

    @MockitoBean
    TelegramSender telegramSender;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registerKafkaProperties(registry, MOCK_REGISTRY, TOPIC, DLT_TOPIC, GROUP_ID);
        registerTelegramProperties(registry);
    }

    @BeforeEach
    void stubTelegramApi() {
        wireMock.stubFor(post(urlPathMatching("/bot.*/setMyCommands"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true,\"result\":true}")));
    }

    @Test
    @DisplayName("Невалидное событие (id=0, пустые чаты) не вызывает Telegram — уходит в DLT")
    void shouldNotCallTelegramOnInvalidEvent() throws Exception {
        ProcessedLinkUpdateEvent invalidEvent = ProcessedLinkUpdateEvent.newBuilder()
                .setId(0L)
                .setUrl("https://github.com")
                .setDescription("Some update")
                .setTgChatIds(List.of())
                .setPriority("HIGH")
                .build();

        publish(TOPIC, MOCK_REGISTRY, "0", invalidEvent);

        awaitDltMessage(DLT_TOPIC, Duration.ofSeconds(15));

        verify(telegramSender, never()).sendMessage(anyLong(), anyString());
    }

    private void awaitDltMessage(String dltTopic, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-verifier-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        long deadline = System.currentTimeMillis() + timeout.toMillis();

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(dltTopic));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    return;
                }
            }
        }
        throw new AssertionError("Сообщение не появилось в DLT за отведённое время: " + timeout);
    }
}
