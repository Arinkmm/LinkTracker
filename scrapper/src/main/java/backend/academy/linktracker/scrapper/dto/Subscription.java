package backend.academy.linktracker.scrapper.dto;

import java.util.List;

public record Subscription(Long chatId, Long linkId, List<String> tags) {}
