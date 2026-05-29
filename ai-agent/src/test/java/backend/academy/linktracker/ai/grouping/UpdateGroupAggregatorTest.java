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
        ProcessedLinkUpdateEvent first = event(1L, "https://github.com/test/repo1", "First update", "LOW");
        ProcessedLinkUpdateEvent second = event(2L, "https://github.com/test/repo2", "Second update", "HIGH");
        UpdateGroup group = group(100L, first, second);

        ProcessedLinkUpdateEvent grouped = aggregator.aggregate(group);

        assertThat(grouped.getId()).isEqualTo(1L);
        assertThat(grouped.getTgChatIds()).containsExactly(100L);
        assertThat(grouped.getPriority()).isEqualTo("HIGH");
        assertThat(grouped.getDescription()).isEqualTo("""
                1. id=1
                url=https://github.com/test/repo1
                First update

                2. id=2
                url=https://github.com/test/repo2
                Second update""");
    }

    @Test
    @DisplayName("Группировка: одиночное обновление возвращается без изменений")
    void shouldKeepSingleUpdateUnchanged() {
        ProcessedLinkUpdateEvent event = event(1L, "https://github.com/test/repo", "Only update", "MEDIUM");
        UpdateGroup group = group(100L, event);

        ProcessedLinkUpdateEvent result = aggregator.aggregate(group);

        assertThat(result.getId()).isEqualTo(event.getId());
        assertThat(result.getUrl()).isEqualTo(event.getUrl());
        assertThat(result.getDescription()).isEqualTo(event.getDescription());
        assertThat(result.getTgChatIds()).containsExactly(100L);
        assertThat(result.getPriority()).isEqualTo(event.getPriority());
    }

    private static UpdateGroup group(long chatId, ProcessedLinkUpdateEvent... events) {
        UpdateGroup group = new UpdateGroup(chatId, java.time.Instant.now());
        for (ProcessedLinkUpdateEvent event : events) {
            group.add(event);
        }
        return group;
    }

    private static ProcessedLinkUpdateEvent event(long id, String url, String description, String priority) {
        return ProcessedLinkUpdateEvent.newBuilder()
                .setId(id)
                .setUrl(url)
                .setDescription(description)
                .setTgChatIds(List.of(100L, 200L))
                .setPriority(priority)
                .build();
    }
}
