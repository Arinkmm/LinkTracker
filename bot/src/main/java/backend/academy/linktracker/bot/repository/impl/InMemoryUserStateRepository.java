package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserStateRepository implements UserStateRepository {
    private final Map<Long, State> map = new HashMap<>();

    @Override
    public void save(Long id, State state) {
        map.put(id, state);
    }

    @Override
    public void delete(Long id) {
        map.remove(id);
    }

    @Override
    public Optional<State> findById(Long id) {
        return Optional.ofNullable(map.get(id));
    }
}
