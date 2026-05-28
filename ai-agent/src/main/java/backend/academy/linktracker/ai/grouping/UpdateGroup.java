package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class UpdateGroup {
    private final Instant createdAt;
    private final List<ProcessedLinkUpdateEvent> events;

    UpdateGroup(Instant createdAt) {
        this(createdAt, List.of());
    }

    private UpdateGroup(Instant createdAt, List<ProcessedLinkUpdateEvent> events) {
        this.createdAt = createdAt;
        this.events = List.copyOf(events);
    }

    UpdateGroup add(ProcessedLinkUpdateEvent event) {
        List<ProcessedLinkUpdateEvent> updatedEvents = new ArrayList<>(events);
        updatedEvents.add(event);
        return new UpdateGroup(createdAt, updatedEvents);
    }

    List<ProcessedLinkUpdateEvent> events() {
        return events;
    }

    boolean isExpired(Instant now, long windowMs) {
        return !createdAt.plusMillis(windowMs).isAfter(now);
    }

    int size() {
        return events.size();
    }
}
