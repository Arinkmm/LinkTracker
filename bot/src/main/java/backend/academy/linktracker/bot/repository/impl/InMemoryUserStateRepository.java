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
    public void save(Long userId, State state) {
        map.put(userId, state);
    }

    @Override
    public void delete(Long userId) {
        map.remove(userId);
    }

    @Override
    public Optional<State> findById(Long chatId) {
        return Optional.ofNullable(map.get(chatId));
    }
}
