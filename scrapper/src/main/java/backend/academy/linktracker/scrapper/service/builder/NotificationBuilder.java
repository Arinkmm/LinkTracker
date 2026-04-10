package backend.academy.linktracker.scrapper.service.builder;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;

public interface NotificationBuilder {
    String buildMessage(LinkResponse response);

    boolean supports(LinkResponse response);
}
