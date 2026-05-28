package backend.academy.linktracker.scrapper.service.builder.impl;

import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractNotificationBuilder {
    protected String formatDate(Instant instant, NotificationProperties properties) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(properties.getDateTimeFormat()).withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }
}
