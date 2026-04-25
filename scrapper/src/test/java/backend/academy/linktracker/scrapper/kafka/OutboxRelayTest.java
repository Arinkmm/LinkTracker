package backend.academy.linktracker.scrapper.kafka;

import backend.academy.linktracker.avro.LinkUpdateEvent;
import backend.academy.linktracker.scrapper.configuration.KafkaIntegrationEnvironment;
import backend.academy.linktracker.scrapper.configuration.SharedKafkaContainer;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import backend.academy.linktracker.scrapper.service.notifier.impl.kafka.OutboxRelay;
import backend.academy.linktracker.scrapper.service.user.ChatService;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import backend.academy.linktracker.scrapper.service.user.OutboxMessageService;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxRelayTest extends KafkaIntegrationEnvironment {
    @MockitoBean
    private TaskScheduler taskScheduler;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private OutboxMessageService outboxMessageService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ChatService chatService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testLinkId;
    private String testTopic;
    private Long currentChatId;

    @BeforeEach
    void setUp() {
        testTopic = "outbox.test." + UUID.randomUUID();
        currentChatId = ThreadLocalRandom.current().nextLong(1000, 1000000);
        ReflectionTestUtils.setField(kafkaProperties, "topic", testTopic);

        transactionTemplate.executeWithoutResult(status -> {
            chatService.registerChat(currentChatId);

            AddLinkRequest request = new AddLinkRequest();
            request.setLink(URI.create("https://github.com/user/test-repo-" + UUID.randomUUID()));
            request.setTags(List.of());
            request.setFilters(List.of());

            testLinkId = linkService.addLink(currentChatId, request).getId();
        });
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE subscriptions, outbox_messages, links, chats CASCADE");
    }

    @Test
    @DisplayName("Успешная пересылка сообщения при валидном Avro-пайлоаде")
    void shouldRelayMessageToKafkaWhenPayloadIsValid() {
        String validPayload = createPayload(testLinkId, "https://github.com/test");

        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, validPayload, OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<LinkUpdateEvent> events = consumeEvents(testTopic, 1, Duration.ofSeconds(15));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getUrl().toString()).isEqualTo("https://github.com/test");
    }

    @Test
    @DisplayName("Пересылка нескольких сообщений за один запуск")
    void shouldRelayMultipleMessagesSuccessfully() {
        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "url-1"), OutboxStatus.NEW);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "url-2"), OutboxStatus.NEW);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "url-3"), OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<LinkUpdateEvent> events = consumeEvents(testTopic, 3, Duration.ofSeconds(20));
        assertThat(events).hasSize(3);
    }

    @Test
    @DisplayName("Relay игнорирует сообщения со статусом SENT")
    void shouldIgnoreAlreadyProcessedMessages() {
        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "old"), OutboxStatus.SENT);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "new"), OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<LinkUpdateEvent> events = consumeEvents(testTopic, 1, Duration.ofSeconds(10));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getUrl().toString()).isEqualTo("new");
    }

    @Test
    @DisplayName("Статус сообщения в БД меняется после отправки")
    void shouldUpdateStatusToProcessedAfterSuccessfulSend() {
        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "status-check"), OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<OutboxMessageEntity> remainingNew = outboxMessageService.getOutboxMessages(0, 10);
        assertThat(remainingNew)
            .filteredOn(m -> m.getStatus() == OutboxStatus.NEW)
            .isEmpty();
    }

    private String createPayload(Long id, String url) {
        return String.format(
            "{\"id\":%d, \"url\":\"%s\", \"description\":\"Update detected\", \"tgChatIds\":[%d]}",
            id, url, currentChatId
        );
    }

    private void saveOutboxMessage(Long linkId, String payload, OutboxStatus status) {
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setLinkId(linkId);
        message.setPayload(payload);
        message.setStatus(status);
        outboxMessageService.addMessage(message);
    }

    private List<LinkUpdateEvent> consumeEvents(String topic, int expectedCount, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafkaContainer.INSTANCE.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        List<LinkUpdateEvent> results = new ArrayList<>();
        try (KafkaConsumer<String, LinkUpdateEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long end = System.currentTimeMillis() + timeout.toMillis();

            while (System.currentTimeMillis() < end && results.size() < expectedCount) {
                var records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> results.add(r.value()));
            }
        }
        return results;
    }
}
