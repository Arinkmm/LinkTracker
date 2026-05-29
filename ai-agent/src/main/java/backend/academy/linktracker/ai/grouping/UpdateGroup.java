package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class UpdateGroup {
    private final long chatId;
    private final Instant createdAt;
    private final List<ProcessedLinkUpdateEvent> events = new ArrayList<>();

    UpdateGroup(long chatId, Instant createdAt) {
        this.chatId = chatId;
        this.createdAt = createdAt;
    }

    void add(ProcessedLinkUpdateEvent event) {
        events.add(event);
    }

    long chatId() {
        return chatId;
    }

    List<ProcessedLinkUpdateEvent> events() {
        return List.copyOf(events);
    }

    boolean isExpired(Instant now, long windowMs) {
        return !createdAt.plusMillis(windowMs).isAfter(now);
    }

    int size() {
        return events.size();
    }
}
