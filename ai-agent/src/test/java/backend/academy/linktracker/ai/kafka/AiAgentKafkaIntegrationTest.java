package backend.academy.linktracker.ai.kafka;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.configuration.AiAgentKafkaTestEnvironment;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AiAgentKafkaIntegrationTest extends AiAgentKafkaTestEnvironment {

    @Test
    @DisplayName("Kafka: корректное raw-событие обрабатывается и публикуется в processed-topic")
    void shouldProcessValidRawUpdate() throws Exception {
        stubAiSummary("AI generated summary");
        String body = "Critical update is long enough to require AI summarization from external API";
        RawLinkUpdateEvent rawEvent = RawLinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription("""
                        Issue
                        Название: Bug report
                        Автор: real-user
                        Время: 2026-05-25 10:00 UTC
                        Описание: %s
                        ────────────────
                        """.formatted(body))
                .setAuthor("real-user")
                .setTgChatIds(List.of(100L, 200L))
                .build();

        publishAvro(RAW_TOPIC, String.valueOf(rawEvent.getId()), rawEvent);

        ProcessedLinkUpdateEvent processed = consumeProcessed(String.valueOf(rawEvent.getId()), Duration.ofSeconds(20));
        assertThat(processed.getDescription())
                .contains("Issue")
                .contains("Название: Bug report")
                .contains("Автор: real-user")
                .contains("Описание: AI generated summary")
                .doesNotContain(body);
        assertThat(processed.getTgChatIds()).hasSize(1).containsAnyOf(100L, 200L);
        assertThat(processed.getPriority()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Kafka: некорректное сообщение уходит в DLT и не роняет consumer")
    void shouldKeepConsumingAfterInvalidMessage() throws Exception {
        String invalidKey = "invalid-" + UUID.randomUUID();
        publishInvalid(RAW_TOPIC, invalidKey);

        awaitRawDltMessage(invalidKey, Duration.ofSeconds(15));

        RawLinkUpdateEvent validEvent = RawLinkUpdateEvent.newBuilder()
                .setId(2L)
                .setUrl("https://github.com/test/repo")
                .setDescription("Useful update")
                .setAuthor("real-user")
                .setTgChatIds(List.of(300L))
                .build();
        publishAvro(RAW_TOPIC, String.valueOf(validEvent.getId()), validEvent);

        ProcessedLinkUpdateEvent processed =
                consumeProcessed(String.valueOf(validEvent.getId()), Duration.ofSeconds(20));
        assertThat(processed.getDescription()).isEqualTo("Useful update");
    }

    @Test
    @DisplayName("Kafka: отфильтрованное raw-событие не публикуется в processed-topic")
    void shouldNotPublishFilteredRawUpdate() throws Exception {
        RawLinkUpdateEvent filteredEvent = RawLinkUpdateEvent.newBuilder()
                .setId(3L)
                .setUrl("https://github.com/test/repo")
                .setDescription("This update contains spam content")
                .setAuthor("real-user")
                .setTgChatIds(List.of(400L))
                .build();

        publishAvro(RAW_TOPIC, String.valueOf(filteredEvent.getId()), filteredEvent);

        assertNoProcessed(String.valueOf(filteredEvent.getId()), Duration.ofSeconds(2));
    }

    private static void stubAiSummary(String summary) {
        wireMock.stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "choices": [
                                    {
                                      "message": {
                                        "role": "assistant",
                                        "content": "%s"
                                      }
                                    }
                                  ]
                                }
                                """.formatted(summary))));
    }
}
