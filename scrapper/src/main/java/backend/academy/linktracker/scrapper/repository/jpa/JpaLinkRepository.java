package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.LinkEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Query(
            "SELECT l FROM LinkEntity l WHERE l.lastChecked IS NULL OR l.lastChecked < :threshold ORDER BY l.lastChecked ASC NULLS FIRST")
    Page<LinkEntity> findStaleLinks(@Param("threshold") Instant threshold, Pageable pageable);
}
