package backend.academy.linktracker.bot.repository;

import java.net.URI;
import java.util.Optional;

public interface UrlRepository {
    void save(Long id, URI url);

    void delete(Long id);

    Optional<URI> findById(Long id);
}
