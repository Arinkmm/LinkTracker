package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.State;
import java.util.Optional;

public interface UserStateRepository {
    void save(Long id, State state);

    void delete(Long id);

    Optional<State> findById(Long id);
}
