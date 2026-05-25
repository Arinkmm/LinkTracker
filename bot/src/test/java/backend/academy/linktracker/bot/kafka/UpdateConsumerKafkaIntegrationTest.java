package backend.academy.linktracker.bot.kafka;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.bot.configuration.BotKafkaTestEnvironment;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import java.util.List;
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
class UpdateConsumerKafkaIntegrationTest extends BotKafkaTestEnvironment {
    private static final String MOCK_REGISTRY = "mock://bot-test-scope";
    private static final String TOPIC = "link.processed-updates-bot-test";
    private static final String DLT_TOPIC = TOPIC + ".DLT";
    private static final String GROUP_ID = "bot-test-group";

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

        wireMock.stubFor(post(urlPathMatching("/bot.*/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true,\"result\":{\"message_id\":1,\"chat\":{\"id\":1},\"text\":\"ok\"}}")));
    }

    @Test
    @DisplayName("Consumer вызывает TelegramSender.sendMessage для каждого chatId из события")
    void shouldCallTelegramSenderForEachChatId() throws Exception {
        long linkId = 42L;
        String description = "New GitHub issue: Bug report";
        List<Long> chatIds = List.of(100L, 200L, 300L);

        ProcessedLinkUpdateEvent event = ProcessedLinkUpdateEvent.newBuilder()
                .setId(linkId)
                .setUrl("https://github.com/user/repo")
                .setDescription(description)
                .setTgChatIds(chatIds)
                .setPriority("HIGH")
                .build();

        publish(TOPIC, MOCK_REGISTRY, String.valueOf(linkId), event);

        chatIds.forEach(chatId -> verify(telegramSender, timeout(10_000)).sendMessage(chatId, description));
    }

    @Test
    @DisplayName("Consumer вызывает sendMessage ровно один раз на chatId (не дублирует)")
    void shouldSendTelegramMessageExactlyOnce() throws Exception {
        ProcessedLinkUpdateEvent event = ProcessedLinkUpdateEvent.newBuilder()
                .setId(99L)
                .setUrl("https://github.com/user/repo2")
                .setDescription("Duplicate delivery simulation")
                .setTgChatIds(List.of(55L))
                .setPriority("HIGH")
                .build();

        publish(TOPIC, MOCK_REGISTRY, String.valueOf(event.getId()), event);

        verify(telegramSender, timeout(10_000).times(1)).sendMessage(55L, "Duplicate delivery simulation");
        verify(telegramSender, after(1_500).times(1)).sendMessage(55L, "Duplicate delivery simulation");
    }
}
