package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.orm.entity.LinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.projection.TrackedLinkSourceCount;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Query(
            "SELECT l FROM LinkEntity l WHERE l.lastChecked IS NULL OR l.lastChecked < :threshold ORDER BY l.lastChecked ASC NULLS FIRST")
    Page<LinkEntity> findStaleLinks(@Param("threshold") Instant threshold, Pageable pageable);

    @Query(value = """
        SELECT
            CASE
                WHEN host = 'github.com' THEN 'github'
                WHEN host = 'stackoverflow.com' OR host LIKE '%.stackoverflow.com' THEN 'stackoverflow'
                ELSE 'other'
            END AS "trackedSource",
            COUNT(*) AS total
        FROM (
            SELECT split_part(regexp_replace(lower(url), '^https?://(www\\.)?', ''), '/', 1) AS host
            FROM links
        ) hosts
        GROUP BY 1
        """, nativeQuery = true)
    List<TrackedLinkSourceCount> countBySource();

    @Modifying
    @Query("""
        DELETE FROM LinkEntity l
        WHERE l.id = :id
          AND NOT EXISTS (SELECT 1 FROM SubscriptionEntity s WHERE s.linkId = :id)
        """)
    void removeIfOrphan(@Param("id") Long id);

    @Modifying
    @Query("""
        DELETE FROM LinkEntity l
        WHERE l.id
          NOT IN (SELECT s.linkId FROM SubscriptionEntity s)
        """)
    void removeOrphans();

    @Modifying
    @Query("UPDATE LinkEntity l SET l.lastChecked = :newTime WHERE l.id = :id")
    void updateLastCheckedAt(@Param("id") Long id, @Param("newTime") Instant newTime);
}
