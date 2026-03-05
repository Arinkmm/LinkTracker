package backend.academy.linktracker.bot.service.handler;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.exception.ScrapperApiException;
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

    public void handle(Long userId, String text, State state) {
        switch (state) {
            case WAITING_URL -> handleWaitingUrl(userId, text);
            case NEEDED_TAGS -> handleNeededTags(userId, text);
            case WAITING_TAGS -> handleWaitingTags(userId, text);
            default -> throw new IllegalStateException("Invalid state for track: " + state);
        }
    }

    private void handleWaitingUrl(Long userId, String text) {
        if (urlValidator.isValid(text)) {
            userService.saveUrl(userId, text);
            userService.saveState(userId, State.NEEDED_TAGS);
            telegramSender.sendMessage(userId, properties.getMessages().getTagsOffer());
        } else {
            telegramSender.sendMessage(userId, properties.getMessages().getInvalidUrl());
        }
    }

    private void handleNeededTags(Long userId, String text) {
        if ("да".equalsIgnoreCase(text)) {
            userService.saveState(userId, State.WAITING_TAGS);
            telegramSender.sendMessage(userId, properties.getMessages().getTagsPrompt());
        } else if ("нет".equalsIgnoreCase(text)) {
            saveLink(userId, List.of());
        } else {
            telegramSender.sendMessage(userId, properties.getMessages().getInvalidTagsAnswer());
        }
    }

    private void handleWaitingTags(Long userId, String text) {
        List<String> tags = tagsParser.parseTags(text);
        saveLink(userId, tags);
    }

    private void saveLink(Long userId, List<String> tags) {
        String url = userService.findUrlById(userId).orElseThrow();
        client.addLink(userId, url, tags, List.of());
        userService.deleteState(userId);
        userService.deleteUrl(userId);

        String message = tags.isEmpty()
            ? properties.getMessages().getLinkAdded()
            : properties.getMessages().getLinkAddedWithTags();

        telegramSender.sendMessage(userId, message);
    }
}
