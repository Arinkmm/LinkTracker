package backend.academy.linktracker.bot.service.handler.state;

import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StateHandler {
    private final Map<State, StateProcessor> processorMap;
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final MessagesProperties properties;

    public StateHandler(
            List<StateProcessor> stateProcessors,
            UserService userService,
            TelegramSender telegramSender,
            MessagesProperties properties) {
        this.processorMap = new HashMap<>();
        for (StateProcessor processor : stateProcessors) {
            for (State state : processor.getSupportedStates()) {
                processorMap.put(state, processor);
            }
        }
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.properties = properties;
    }

    public boolean hasState(Long id) {
        return userService.findStateById(id).filter(s -> s != State.OK).isPresent();
    }

    public void handle(Long id, String text) {
        State state = userService.findStateById(id).orElse(State.OK);

        log.atDebug()
                .addKeyValue("id", id)
                .addKeyValue("state", state)
                .addKeyValue("text", text)
                .log("Processing State Machine state");

        try {
            StateProcessor processor = processorMap.get(state);
            if (processor != null) {
                processor.handle(id, text, state);
            } else {
                handleUnknownState(id);
            }
        } catch (ApiException e) {
            log.atError()
                    .addKeyValue("id", id)
                    .addKeyValue("state", state)
                    .addKeyValue("text", text)
                    .addKeyValue("error_code", e.getApiError().getCode())
                    .addKeyValue("description", e.getApiError().getDescription())
                    .log("State Machine failed");

            telegramSender.sendMessage(id, e.getApiError().getDescription());
            userService.deleteState(id);
        }
    }

    private void handleUnknownState(Long id) {
        telegramSender.sendMessage(id, properties.getStateError());
        userService.deleteState(id);
    }
}
