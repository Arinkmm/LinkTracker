package backend.academy.linktracker.scrapper.service.builder.impl;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import backend.academy.linktracker.scrapper.service.builder.NotificationBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationBuilderService {
    private final NotificationProperties properties;
    private final List<NotificationBuilder> builders;

    public String buildError(Link link) {
        return properties.getError().getTitle() + "\n\n" + link.url() + "\n";
    }

    public String buildMessage(LinkResponse response) {
        String content = builders.stream()
                .filter(b -> b.supports(response))
                .findFirst()
                .map(b -> b.buildMessage(response))
                .orElse("");

        return properties.getDefaulting().getTitle() + "\n\n" + content;
    }
}
