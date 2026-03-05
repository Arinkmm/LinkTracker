package backend.academy.linktracker.bot.service.handler;

import backend.academy.linktracker.bot.exception.ScrapperApiException;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StateHandler {
    private final UserService userService;
    private final TrackStateHandler trackHandler;
    private final UntrackStateHandler untrackHandler;
    private final TelegramSender telegramSender;
    private final CommandProperties properties;

    public boolean hasState(Long userId) {
        return userService.findStateById(userId)
            .filter(s -> s != State.OK)
            .isPresent();
    }

    public void handle(Long userId, String text) {
        State state = userService.findStateById(userId).orElse(State.OK);

        try {
            switch (state) {
                case WAITING_URL, NEEDED_TAGS, WAITING_TAGS ->
                    trackHandler.handle(userId, text, state);
                case WAITING_UNTRACKING_URL ->
                    untrackHandler.handle(userId, text);
                default -> handleUnknownState(userId, state);
            }
        } catch (ScrapperApiException e) {
            telegramSender.sendMessage(userId, e.getApiError().description());
            userService.deleteState(userId);
        }
    }

    private void handleUnknownState(Long userId, State state) {
        telegramSender.sendMessage(userId, properties.getMessages().getStateError());
        userService.deleteState(userId);
    }
}
