package backend.academy.linktracker.bot.service.handler.state;

import static backend.academy.linktracker.bot.model.State.NEEDED_TAGS;
import static backend.academy.linktracker.bot.model.State.WAITING_TAGS;
import static backend.academy.linktracker.bot.model.State.WAITING_URL;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.properties.ResponsesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.TagsParser;
import backend.academy.linktracker.bot.util.url.validator.UrlValidator;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackStateHandler implements StateProcessor {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ScrapperClient client;
    private final UrlValidator urlValidator;
    private final TagsParser tagsParser;
    private final MessagesProperties messagesProperties;
    private final ResponsesProperties responsesProperties;

    @Override
    public void handle(Long id, String text, State state) {
        log.atDebug()
                .addKeyValue("id", id)
                .addKeyValue("state", state)
                .addKeyValue("text", text)
                .log("Track State Machine step");

        switch (state) {
            case WAITING_URL -> handleWaitingUrl(id, text);
            case NEEDED_TAGS -> handleNeededTags(id, text);
            case WAITING_TAGS -> handleWaitingTags(id, text);
            default -> throw new IllegalStateException("Invalid state for track: " + state);
        }
    }

    @Override
    public List<State> getSupportedStates() {
        return List.of(WAITING_TAGS, NEEDED_TAGS, WAITING_URL);
    }

    private void handleWaitingUrl(Long id, String text) {
        URI uri = URI.create(text);
        if (urlValidator.isValid(uri)) {
            userService.saveUrl(id, uri);
            userService.saveState(id, NEEDED_TAGS);
            telegramSender.sendMessage(id, messagesProperties.getTagsOffer());
        } else {
            telegramSender.sendMessage(id, messagesProperties.getInvalidUrl());
        }
    }

    private void handleNeededTags(Long id, String text) {
        if (responsesProperties.getNeededTags().equalsIgnoreCase(text)) {
            userService.saveState(id, WAITING_TAGS);
            telegramSender.sendMessage(id, messagesProperties.getTagsPrompt());
        } else if (responsesProperties.getNotNeededTags().equalsIgnoreCase(text)) {
            saveLink(id, List.of());
        } else {
            telegramSender.sendMessage(id, messagesProperties.getInvalidTagsAnswer());
        }
    }

    private void handleWaitingTags(Long id, String text) {
        List<String> tags = tagsParser.parseTags(text);
        saveLink(id, tags);
    }

    private void saveLink(Long id, List<String> tags) {
        log.atInfo()
                .addKeyValue("id", id)
                .addKeyValue("url", userService.findUrlById(id))
                .addKeyValue("tags_count", tags.size())
                .log("Saving link to scrapper");

        URI url = userService.findUrlById(id).orElseThrow();
        client.addLink(id, url, tags, List.of());
        userService.deleteState(id);
        userService.deleteUrl(id);

        String message = tags.isEmpty() ? messagesProperties.getLinkAdded() : messagesProperties.getLinkAddedWithTags();

        telegramSender.sendMessage(id, message);
    }
}
