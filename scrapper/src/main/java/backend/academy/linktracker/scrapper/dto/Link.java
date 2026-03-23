package backend.academy.linktracker.scrapper.dto;

import java.net.URI;
import java.time.Instant;

public record Link(Long id, URI url, Instant lastChecked) {}
