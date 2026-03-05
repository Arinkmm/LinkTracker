package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.dto.bot.UserDto;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, UserDto> map = new HashMap<>();

    @Override
    public void save(Long userId) {
        map.put(userId, new UserDto(userId));
    }

    @Override
    public boolean exists(Long userId) {
        return map.containsKey(userId);
    }

    @Override
    public void delete(Long userId) {
        map.remove(userId);
    }
}
