package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.Link;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {
    Link save(URI url);

    List<Link> findByIds(List<Long> ids);

    Optional<Link> findByUrl(URI url);

    void remove(Long id);

    void removeIfOrphan(Long id);

    void removeOrphans();

    List<Link> findStaleLinks(Instant threshold, int page, int size);

    void updateLastChecked(Long id, Instant newTime);
}
