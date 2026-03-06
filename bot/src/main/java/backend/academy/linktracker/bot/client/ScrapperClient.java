package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import java.util.List;

public interface ScrapperClient {
    void registerChat(Long id);

    void deleteChat(Long id);

    LinkResponse addLink(Long id, String url, List<String> tags, List<String> filters);

    LinkResponse removeLink(Long id, String url);

    ListLinksResponse getLinks(Long id);
}
