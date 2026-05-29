package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.ai.prioritization.UpdatePriority;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class UpdateGroupAggregator {

    public ProcessedLinkUpdateEvent aggregate(UpdateGroup group) {
        List<ProcessedLinkUpdateEvent> events = group.events();
        if (events.isEmpty()) {
            throw new IllegalArgumentException("events must not be empty");
        }

        ProcessedLinkUpdateEvent first = events.getFirst();
        return ProcessedLinkUpdateEvent.newBuilder()
                .setId(first.getId())
                .setUrl(first.getUrl())
                .setDescription(events.size() == 1 ? first.getDescription() : numberedDescription(events))
                .setTgChatIds(List.of(group.chatId()))
                .setPriority(maxPriority(events).name())
                .build();
    }

    private String numberedDescription(List<ProcessedLinkUpdateEvent> events) {
        return IntStream.range(0, events.size())
                .mapToObj(
                        index -> "%d. %s".formatted(index + 1, events.get(index).getDescription()))
                .collect(Collectors.joining("\n"));
    }

    private UpdatePriority maxPriority(List<ProcessedLinkUpdateEvent> events) {
        UpdatePriority priority = UpdatePriority.LOW;
        for (ProcessedLinkUpdateEvent event : events) {
            priority = UpdatePriority.max(priority.name(), event.getPriority());
        }
        return priority;
    }
}
