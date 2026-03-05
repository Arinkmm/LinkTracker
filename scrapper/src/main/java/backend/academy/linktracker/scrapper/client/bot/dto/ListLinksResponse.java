package backend.academy.linktracker.scrapper.client.bot.dto;

import java.util.List;

public record ListLinksResponse(List<LinkResponse> links, int size) {}
