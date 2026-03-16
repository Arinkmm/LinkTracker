package backend.academy.linktracker.bot.service.handler.state;

import backend.academy.linktracker.bot.model.State;
import java.util.List;

public interface StateProcessor {
    void handle(Long id, String text, State state);

    List<State> getSupportedStates();
}
