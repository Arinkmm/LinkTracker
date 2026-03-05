package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.client.type", havingValue = "http")
@RequiredArgsConstructor
public class ScrapperHttpClient implements ScrapperClient {
    private final RestClient restClient;

    @Override
    public void registerChat(Long chatId) {
        restClient
            .method(HttpMethod.POST)
            .uri("/tg-chat/{id}", chatId)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void deleteChat(Long chatId) {
        restClient
            .method(HttpMethod.DELETE)
            .uri("/tg-chat/{id}", chatId)
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public LinkResponse addLink(Long chatId, String url, List<String> tags, List<String> filters) {
        return restClient
            .method(HttpMethod.POST)
            .uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .body(new AddLinkRequest(url, tags, filters))
            .retrieve()
            .body(LinkResponse.class);
    }

    @Override
    public LinkResponse removeLink(Long chatId, String url) {
        return restClient
            .method(HttpMethod.DELETE)
            .uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .body(new RemoveLinkRequest(url))
            .retrieve()
            .body(LinkResponse.class);
    }

    @Override
    public ListLinksResponse getLinks(Long chatId) {
        return restClient
            .method(HttpMethod.GET)
            .uri("/links")
            .header("Tg-Chat-Id", String.valueOf(chatId))
            .retrieve()
            .body(ListLinksResponse.class);
    }
}
