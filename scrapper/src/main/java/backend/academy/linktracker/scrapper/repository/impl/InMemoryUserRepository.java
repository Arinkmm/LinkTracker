package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.dto.bot.UserDto;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, UserDto> map = new ConcurrentHashMap<>();

    @Override
    public void save(Long id) {
        map.put(id, new UserDto(id));
    }

    @Override
    public boolean exists(Long id) {
        return map.containsKey(id);
    }

    @Override
    public void delete(Long id) {
        map.remove(id);
    }
}
