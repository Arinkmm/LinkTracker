package backend.academy.linktracker.ai.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateGroupAggregatorTest {
    private final UpdateGroupAggregator aggregator = new UpdateGroupAggregator();

    @Test
    @DisplayName("Группировка: несколько обновлений объединяются в нумерованный список")
    void shouldGroupSeveralUpdates() {
        ProcessedLinkUpdateEvent first = event(1L, "First update", "LOW");
        ProcessedLinkUpdateEvent second = event(2L, "Second update", "HIGH");

        ProcessedLinkUpdateEvent grouped = aggregator.aggregate(List.of(first, second));

        assertThat(grouped.getId()).isEqualTo(1L);
        assertThat(grouped.getTgChatIds()).containsExactly(100L);
        assertThat(grouped.getPriority()).isEqualTo("HIGH");
        assertThat(grouped.getDescription()).isEqualTo("""
                1. First update
                2. Second update""");
    }

    @Test
    @DisplayName("Группировка: одиночное обновление возвращается без изменений")
    void shouldKeepSingleUpdateUnchanged() {
        ProcessedLinkUpdateEvent event = event(1L, "Only update", "MEDIUM");

        ProcessedLinkUpdateEvent result = aggregator.aggregate(List.of(event));

        assertThat(result).isSameAs(event);
    }

    private static ProcessedLinkUpdateEvent event(long id, String description, String priority) {
        return ProcessedLinkUpdateEvent.newBuilder()
                .setId(id)
                .setUrl("https://github.com/test/repo")
                .setDescription(description)
                .setTgChatIds(List.of(100L))
                .setPriority(priority)
                .build();
    }
}
