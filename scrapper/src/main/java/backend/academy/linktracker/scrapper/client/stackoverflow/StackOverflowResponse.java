package backend.academy.linktracker.scrapper.client.stackoverflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackOverflowResponse(@JsonProperty("items") List<Item> items) {
    public record Item(@JsonProperty("last_activity_date") long lastActivityDate) {}

    public Instant lastActivity() {
        return items != null && !items.isEmpty()
                ? Instant.ofEpochSecond(items.getFirst().lastActivityDate())
                : Instant.EPOCH;
    }
}
