package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;

public interface UserRepository {
    void save(User user);
}
