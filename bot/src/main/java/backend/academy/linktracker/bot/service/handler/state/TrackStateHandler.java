package backend.academy.linktracker.bot.service.handler.state;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.TagsParser;
import backend.academy.linktracker.bot.util.UrlValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackStateHandler {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ScrapperClient client;
    private final CommandProperties properties;
    private final UrlValidator urlValidator;
    private final TagsParser tagsParser;

    public void handle(Long id, String text, State state) {
        switch (state) {
            case WAITING_URL -> handleWaitingUrl(id, text);
            case NEEDED_TAGS -> handleNeededTags(id, text);
            case WAITING_TAGS -> handleWaitingTags(id, text);
            default -> throw new IllegalStateException("Invalid state for track: " + state);
        }
    }

    private void handleWaitingUrl(Long id, String text) {
        if (urlValidator.isValid(text)) {
            userService.saveUrl(id, text);
            userService.saveState(id, State.NEEDED_TAGS);
            telegramSender.sendMessage(id, properties.getMessages().getTagsOffer());
        } else {
            telegramSender.sendMessage(id, properties.getMessages().getInvalidUrl());
        }
    }

    private void handleNeededTags(Long id, String text) {
        if ("да".equalsIgnoreCase(text)) {
            userService.saveState(id, State.WAITING_TAGS);
            telegramSender.sendMessage(id, properties.getMessages().getTagsPrompt());
        } else if ("нет".equalsIgnoreCase(text)) {
            saveLink(id, List.of());
        } else {
            telegramSender.sendMessage(id, properties.getMessages().getInvalidTagsAnswer());
        }
    }

    private void handleWaitingTags(Long id, String text) {
        List<String> tags = tagsParser.parseTags(text);
        saveLink(id, tags);
    }

    private void saveLink(Long id, List<String> tags) {
        String url = userService.findUrlById(id).orElseThrow();
        client.addLink(id, url, tags, List.of());
        userService.deleteState(id);
        userService.deleteUrl(id);

        String message = tags.isEmpty()
            ? properties.getMessages().getLinkAdded()
            : properties.getMessages().getLinkAddedWithTags();

        telegramSender.sendMessage(id, message);
    }
}
