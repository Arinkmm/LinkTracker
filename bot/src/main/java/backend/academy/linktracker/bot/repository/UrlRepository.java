package backend.academy.linktracker.bot.repository;

import java.util.Optional;

public interface UrlRepository {
    void save(Long userId, String url);

    void delete(Long userId);

    Optional<String> findById(Long userId);
}
