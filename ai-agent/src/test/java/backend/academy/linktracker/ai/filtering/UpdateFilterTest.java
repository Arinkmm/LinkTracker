package backend.academy.linktracker.ai.filtering;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateFilterTest {
    private UpdateFilter filter;

    @BeforeEach
    void setUp() {
        AiAgentProperties.Api api = new AiAgentProperties.Api("url", "token", "model", Duration.ofSeconds(1), "prompt");
        AiAgentProperties.Summarization summ = new AiAgentProperties.Summarization(10, api);
        AiAgentProperties.Filtering filt = new AiAgentProperties.Filtering(List.of("spam"), List.of("bot-user"), 20);
        AiAgentProperties.Labels labels = new AiAgentProperties.Labels("Description: ", "────────────────");
        AiAgentProperties properties = new AiAgentProperties(filt, summ, labels);
        filter = new UpdateFilter(properties);
    }

    @Test
    @DisplayName("Фильтрация: обновление со стоп-словом игнорируется")
    void shouldFilterByStopWord() {
        RawLinkUpdateEvent event = event("This update contains SPAM content", "real-user");

        assertThat(filter.shouldProcess(event)).isFalse();
    }

    @Test
    @DisplayName("Фильтрация: обновление от исключённого автора игнорируется")
    void shouldFilterByAuthor() {
        RawLinkUpdateEvent event = event("This update is long enough to pass length filter", "bot-user");

        assertThat(filter.shouldProcess(event)).isFalse();
    }

    @Test
    @DisplayName("Фильтрация: короткое обновление игнорируется")
    void shouldFilterByMinimumLength() {
        RawLinkUpdateEvent event = event("too short", "real-user");

        assertThat(filter.shouldProcess(event)).isFalse();
    }

    @Test
    @DisplayName("Фильтрация: валидное обновление проходит обработку")
    void shouldPassValidUpdate() {
        RawLinkUpdateEvent event = event("This update is useful and long enough", "real-user");

        assertThat(filter.shouldProcess(event)).isTrue();
    }

    private static RawLinkUpdateEvent event(String description, String author) {
        return RawLinkUpdateEvent.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/test/repo")
                .setDescription(description)
                .setAuthor(author)
                .setTgChatIds(List.of(100L))
                .build();
    }
}
