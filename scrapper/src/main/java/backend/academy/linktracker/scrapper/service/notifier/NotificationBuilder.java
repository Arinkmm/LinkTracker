package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class NotificationBuilder {
    private final NotificationProperties properties;

    public String buildMessage(String url) {
        return properties.getTitle() + "\n" +
            String.format(properties.getBody(), url);
    }
}
