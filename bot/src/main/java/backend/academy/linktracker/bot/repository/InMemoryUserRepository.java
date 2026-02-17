package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> map = new HashMap<>();

    @Override
    public void save(User user) {
        map.put(user.chatId(), user);
    }

    @Override
    public boolean exists(Long chatId) {
        return map.containsKey(chatId);
    }
}
