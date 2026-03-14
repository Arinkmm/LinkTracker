package backend.academy.linktracker.scrapper.dto;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public record LinkDto(Long id, URI url, List<String> tags, List<String> filters, Instant lastChecked) {
    public LinkDto withLastChecked(Instant newTime) {
        return new LinkDto(id, url, tags, filters, newTime);
    }
}
