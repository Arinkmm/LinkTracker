package backend.academy.linktracker.bot.repository;

import java.util.Optional;

public interface UrlRepository {
    void save(Long id, String url);

    void delete(Long id);

    Optional<String> findById(Long id);
}
