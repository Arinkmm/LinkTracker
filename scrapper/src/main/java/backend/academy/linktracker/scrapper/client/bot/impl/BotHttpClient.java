package backend.academy.linktracker.scrapper.client.bot.impl;

import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "app.client.type", havingValue = "http")
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
