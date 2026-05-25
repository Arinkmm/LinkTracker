package backend.academy.linktracker.ai.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.filtering.UpdateFilter;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.summarization.AiApiUpdateSummarizer;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateProcessingServiceTest {
    private static final String SUMMARY = "AI summary";
    private static final String DESCRIPTION_LABEL = "Описание: ";

    private UpdateProcessingService service;

    @BeforeEach
    void setUp() {
        AiAgentProperties.Api api = new AiAgentProperties.Api("url", "token", "model", Duration.ofSeconds(1), "prompt");
        AiAgentProperties.Summarization summ = new AiAgentProperties.Summarization(30, api);
        AiAgentProperties.Filtering filt = new AiAgentProperties.Filtering(List.of(), List.of(), 0);
        AiAgentProperties.Labels labels = new AiAgentProperties.Labels(DESCRIPTION_LABEL);
        AiAgentProperties properties = new AiAgentProperties(filt, summ, labels);
        AiApiUpdateSummarizer summarizer = mock(AiApiUpdateSummarizer.class);
        when(summarizer.summarize(anyString())).thenReturn(SUMMARY);

        service = new UpdateProcessingService(new UpdateFilter(properties), summarizer, properties);
    }

    @Test
    @DisplayName("Суммаризация: длинный текст сокращается через AI API")
    void shouldSummarizeLongText() {
        RawLinkUpdateEvent event = event("This update is definitely longer than configured summarization threshold");

        Optional<ProcessedLinkUpdateEvent> result = service.process(event);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getDescription()).isEqualTo(SUMMARY);
    }

    @Test
    @DisplayName("Summarization: notification format is preserved while body is summarized")
    void shouldPreserveNotificationFormatWhenSummarizingBody() {
        String body = "This update is definitely longer than configured summarization threshold";
        String description = """
                Issue
                Название: Parser fix
                Автор: octocat
                Время: 2026-05-25 10:00 UTC
                %s%s
                ────────────────
                """.formatted(DESCRIPTION_LABEL, body);

        Optional<ProcessedLinkUpdateEvent> result = service.process(event(description));

        assertThat(result).isPresent();
        ProcessedLinkUpdateEvent processed = result.orElseThrow();
        assertThat(processed.getDescription())
                .contains("Issue")
                .contains("Название: Parser fix")
                .contains("Автор: octocat")
                .contains(DESCRIPTION_LABEL + SUMMARY)
                .doesNotContain(body);
    }

    @Test
    @DisplayName("Суммаризация: короткий текст передаётся без изменений")
    void shouldKeepShortTextUnchanged() {
        String description = "Short useful update";

        Optional<ProcessedLinkUpdateEvent> result = service.process(event(description));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getDescription()).isEqualTo(description);
    }

    private static RawLinkUpdateEvent event(String description) {
        return RawLinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription(description)
                .setAuthor("real-user")
                .setTgChatIds(List.of(100L))
                .build();
    }
}
