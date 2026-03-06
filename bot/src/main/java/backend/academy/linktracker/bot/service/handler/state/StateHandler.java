package backend.academy.linktracker.bot.service.handler.state;

import backend.academy.linktracker.bot.exception.ApiException;
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

    public boolean hasState(Long id) {
        return userService.findStateById(id)
            .filter(s -> s != State.OK)
            .isPresent();
    }

    public void handle(Long id, String text) {
        State state = userService.findStateById(id).orElse(State.OK);

        try {
            switch (state) {
                case WAITING_URL, NEEDED_TAGS, WAITING_TAGS ->
                    trackHandler.handle(id, text, state);
                case WAITING_UNTRACKING_URL ->
                    untrackHandler.handle(id, text);
                default -> handleUnknownState(id);
            }
        } catch (ApiException e) {
            telegramSender.sendMessage(id, e.getApiError().description());
            userService.deleteState(id);
        }
    }

    private void handleUnknownState(Long id) {
        telegramSender.sendMessage(id, properties.getMessages().getStateError());
        userService.deleteState(id);
    }
}
