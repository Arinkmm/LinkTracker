package backend.academy.linktracker.scrapper.dto.bot;

import java.time.Instant;
import java.util.List;

public record LinkDto(Long id, String url, List<String> tags, List<String> filters, Instant lastChecked, Long userId) {
    public LinkDto withLastChecked(Instant newTime) {
        return new LinkDto(id, url, tags, filters, newTime, userId);
    }
}
