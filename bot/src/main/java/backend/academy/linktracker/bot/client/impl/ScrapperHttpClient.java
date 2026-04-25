package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.scrapper-client.type", havingValue = "http")
@RequiredArgsConstructor
public class ScrapperHttpClient implements ScrapperClient {
    private final RestClient restClient;

    @Override
    public void registerChat(Long id) {
        restClient.method(HttpMethod.POST).uri("/tg-chat/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public void deleteChat(Long id) {
        restClient.method(HttpMethod.DELETE).uri("/tg-chat/{id}", id).retrieve().toBodilessEntity();
    }

    @Override
    public LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters) {
        AddLinkRequest request = new AddLinkRequest();
        request.setLink(url);
        request.setTags(tags);

        return restClient
                .method(HttpMethod.POST)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(id))
                .body(request)
                .retrieve()
                .body(LinkResponse.class);
    }

    @Override
    public LinkResponse removeLink(Long id, URI url) {
        RemoveLinkRequest request = new RemoveLinkRequest();
        request.setLink(url);

        return restClient
                .method(HttpMethod.DELETE)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(id))
                .body(request)
                .retrieve()
                .body(LinkResponse.class);
    }

    @Override
    public ListLinksResponse getLinks(Long id) {
        return restClient
                .method(HttpMethod.GET)
                .uri("/links")
                .header("Tg-Chat-Id", String.valueOf(id))
                .retrieve()
                .body(ListLinksResponse.class);
    }
}
