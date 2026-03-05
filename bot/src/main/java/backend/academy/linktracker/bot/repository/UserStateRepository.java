package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.State;
import java.util.Optional;

public interface UserStateRepository {
    void save(Long userId, State state);

    void delete(Long userId);

    Optional<State> findById(Long userId);
}
