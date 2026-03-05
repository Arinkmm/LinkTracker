package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.repository.UrlRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUrlRepository implements UrlRepository {
    private final Map<Long, String> map = new HashMap<>();

    @Override
    public void save(Long userId, String url) {
        map.put(userId, url);
    }

    @Override
    public void delete(Long userId) {
        map.remove(userId);
    }

    @Override
    public Optional<String> findById(Long userId) {
        return Optional.ofNullable(map.get(userId));
    }
}
