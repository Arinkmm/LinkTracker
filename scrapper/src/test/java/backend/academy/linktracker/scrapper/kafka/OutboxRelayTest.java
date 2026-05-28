package backend.academy.linktracker.scrapper.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import backend.academy.linktracker.scrapper.configuration.KafkaIntegrationEnvironment;
import backend.academy.linktracker.scrapper.configuration.SharedKafkaContainer;
import backend.academy.linktracker.scrapper.configuration.TestDatabaseCleaner;
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
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxRelayTest extends KafkaIntegrationEnvironment {
    private static final String TEST_TOPIC = "outbox.test.relay";

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
    private Long currentChatId;

    @BeforeEach
    void setUp() {
        TestDatabaseCleaner.clean(jdbcTemplate);
        currentChatId = ThreadLocalRandom.current().nextLong(1000, 1000000);
        ReflectionTestUtils.setField(kafkaProperties, "topic", TEST_TOPIC);

        transactionTemplate.executeWithoutResult(status -> {
            chatService.registerChat(currentChatId);
            AddLinkRequest request = new AddLinkRequest();
            request.setLink(URI.create("https://github.com/user/test-repo-" + UUID.randomUUID()));
            request.setTags(List.of());
            request.setFilters(List.of());
            testLinkId = linkService.addLink(currentChatId, request).getId();
        });
    }

    @Test
    @DisplayName("Успешная пересылка сообщения при валидном Avro-пейлоаде")
    void shouldRelayMessageToKafkaWhenPayloadIsValid() {
        String uniqueUrl = "https://github.com/test-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(
                status -> saveOutboxMessage(testLinkId, createPayload(testLinkId, uniqueUrl), OutboxStatus.NEW));

        outboxRelay.runRelay();

        List<RawLinkUpdateEvent> events = consumeEvents(TEST_TOPIC, 1, uniqueUrl, Duration.ofSeconds(15));
        assertThat(events).hasSize(1);
        RawLinkUpdateEvent event = events.get(0);
        assertThat(event.getUrl().toString()).isEqualTo(uniqueUrl);
        assertThat(event.getDescription()).isEqualTo("Update detected");
        assertThat(event.getAuthor()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Пересылка нескольких сообщений за один запуск")
    void shouldRelayMultipleMessagesSuccessfully() {
        String urlPrefix = "multi-url-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, createPayload(testLinkId, urlPrefix + "-1"), OutboxStatus.NEW);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, urlPrefix + "-2"), OutboxStatus.NEW);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, urlPrefix + "-3"), OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<RawLinkUpdateEvent> events = consumeEvents(TEST_TOPIC, 3, urlPrefix, Duration.ofSeconds(20));
        assertThat(events).hasSize(3);
    }

    @Test
    @DisplayName("Relay игнорирует сообщения со статусом SENT")
    void shouldIgnoreAlreadyProcessedMessages() {
        String newUrl = "new-url-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            saveOutboxMessage(testLinkId, createPayload(testLinkId, "old-url"), OutboxStatus.SENT);
            saveOutboxMessage(testLinkId, createPayload(testLinkId, newUrl), OutboxStatus.NEW);
        });

        outboxRelay.runRelay();

        List<RawLinkUpdateEvent> events = consumeEvents(TEST_TOPIC, 1, newUrl, Duration.ofSeconds(10));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getUrl().toString()).isEqualTo(newUrl);
        assertThat(events.get(0).getDescription()).isEqualTo("Update detected");
    }

    @Test
    @DisplayName("Статус сообщения меняется на SENT после успешной отправки")
    void shouldUpdateStatusToSentAfterSuccessfulRelay() {
        transactionTemplate.executeWithoutResult(
                status -> saveOutboxMessage(testLinkId, createPayload(testLinkId, "status-check"), OutboxStatus.NEW));

        outboxRelay.runRelay();

        List<OutboxMessageEntity> remaining = outboxMessageService.getOutboxMessages(0, 10);
        assertThat(remaining).filteredOn(m -> m.getStatus() == OutboxStatus.NEW).isEmpty();
    }

    private String createPayload(Long id, String url) {
        return String.format(
                "{\"id\":%d,\"url\":\"%s\",\"description\":\"Update detected\",\"tgChatIds\":[%d]}",
                id, url, currentChatId);
    }

    private void saveOutboxMessage(Long linkId, String payload, OutboxStatus status) {
        OutboxMessageEntity message = new OutboxMessageEntity();
        message.setLinkId(linkId);
        message.setPayload(payload);
        message.setStatus(status);
        outboxMessageService.addMessage(message);
    }

    private List<RawLinkUpdateEvent> consumeEvents(
            String topic, int expectedCount, String urlFilter, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, SharedKafkaContainer.INSTANCE.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        List<RawLinkUpdateEvent> results = new ArrayList<>();
        try (KafkaConsumer<String, RawLinkUpdateEvent> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            long deadline = System.currentTimeMillis() + timeout.toMillis();

            while (System.currentTimeMillis() < deadline && results.size() < expectedCount) {
                ConsumerRecords<String, RawLinkUpdateEvent> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, RawLinkUpdateEvent> record : records) {
                    if (record.value().getUrl().toString().contains(urlFilter)) {
                        results.add(record.value());
                    }
                }
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        }
        return results;
    }
}
