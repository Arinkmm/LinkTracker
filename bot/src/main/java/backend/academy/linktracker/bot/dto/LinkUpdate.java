package backend.academy.linktracker.bot.dto;

import java.util.List;

public record LinkUpdate(long id, String url, String description, List<Long> tgChatIds) {}
