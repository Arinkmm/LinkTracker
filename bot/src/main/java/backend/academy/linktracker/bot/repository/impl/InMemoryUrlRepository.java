package backend.academy.linktracker.bot.repository.impl;

import backend.academy.linktracker.bot.repository.UrlRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUrlRepository implements UrlRepository {
    private final Map<Long, String> map = new ConcurrentHashMap<>();

    @Override
    public void save(Long id, String url) {
        map.put(id, url);
    }

    @Override
    public void delete(Long id) {
        map.remove(id);
    }

    @Override
    public Optional<String> findById(Long id) {
        return Optional.ofNullable(map.get(id));
    }
}
