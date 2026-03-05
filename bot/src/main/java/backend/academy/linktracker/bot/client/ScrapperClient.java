package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.client.dto.LinkResponse;
import backend.academy.linktracker.bot.client.dto.ListLinksResponse;
import java.util.List;

public interface ScrapperClient {
    void registerChat(Long chatId);

    void deleteChat(Long chatId);

    LinkResponse addLink(Long chatId, String url, List<String> tags, List<String> filters);

    LinkResponse removeLink(Long chatId, String url);

    ListLinksResponse getLinks(Long chatId);
}
