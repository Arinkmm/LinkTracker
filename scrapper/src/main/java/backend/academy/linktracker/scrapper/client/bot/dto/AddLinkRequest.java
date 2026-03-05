package backend.academy.linktracker.scrapper.client.bot.dto;

import java.util.List;

public record AddLinkRequest(String link, List<String> tags, List<String> filters) {}
