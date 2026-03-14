package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.net.URI;
import java.util.List;

public interface ScrapperClient {
    void registerChat(Long id);

    void deleteChat(Long id);

    LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters);

    LinkResponse removeLink(Long id, URI url);

    ListLinksResponse getLinks(Long id);
}
