package backend.academy.linktracker.ai.prioritization;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdatePrioritizerTest {
    private UpdatePrioritizer prioritizer;

    @BeforeEach
    void setUp() {
        prioritizer = new UpdatePrioritizer(properties());
    }

    @Test
    @DisplayName("Приоритизация: high-ключевое слово даёт приоритет HIGH")
    void shouldDetectHighPriority() {
        assertThat(prioritizer.prioritize("critical bug fix")).isEqualTo(UpdatePriority.HIGH);
    }

    @Test
    @DisplayName("Приоритизация: обновление без ключевых слов получает приоритет MEDIUM")
    void shouldDetectMediumPriority() {
        assertThat(prioritizer.prioritize("regular useful update")).isEqualTo(UpdatePriority.MEDIUM);
    }

    @Test
    @DisplayName("Приоритизация: low-ключевое слово даёт приоритет LOW")
    void shouldDetectLowPriority() {
        assertThat(prioritizer.prioritize("fix typo in readme")).isEqualTo(UpdatePriority.LOW);
    }

    private static AiAgentProperties properties() {
        AiAgentProperties.Api api = new AiAgentProperties.Api("url", "token", "model", Duration.ofSeconds(1), "prompt");
        AiAgentProperties.Summarization summ = new AiAgentProperties.Summarization(30, api);
        AiAgentProperties.Filtering filt = new AiAgentProperties.Filtering(List.of(), List.of(), 0);
        AiAgentProperties.Labels labels = new AiAgentProperties.Labels("Description: ", "----------------");
        AiAgentProperties.Prioritization prioritization =
                new AiAgentProperties.Prioritization(List.of("critical", "security"), List.of("typo", "docs"));
        AiAgentProperties.Grouping grouping = new AiAgentProperties.Grouping(100, 10);
        return new AiAgentProperties(filt, summ, labels, prioritization, grouping);
    }
}
