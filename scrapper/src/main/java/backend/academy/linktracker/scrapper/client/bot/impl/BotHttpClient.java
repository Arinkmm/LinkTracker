package backend.academy.linktracker.scrapper.client.bot.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class BotHttpClient implements BotClient {
    private final RestClient restClient;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        restClient
                .method(HttpMethod.POST)
                .uri("/updates")
                .body(linkUpdate)
                .retrieve()
                .toBodilessEntity();
    }
}
