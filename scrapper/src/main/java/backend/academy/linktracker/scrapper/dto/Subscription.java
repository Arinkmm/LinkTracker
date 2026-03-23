package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record Subscription(Long userId, Long linkId, List<String> tags) {}
