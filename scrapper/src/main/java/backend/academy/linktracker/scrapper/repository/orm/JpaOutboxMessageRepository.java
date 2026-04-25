package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface JpaOutboxMessageRepository extends JpaRepository<OutboxMessageEntity, Long> {
    @Query("SELECT m FROM OutboxMessageEntity m WHERE m.status IN ('NEW', 'ERROR') AND m.retryCount < :maxRetries")
    List<OutboxMessageEntity> findAllWithLimit(@Param("maxRetries") int maxRetries, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM outbox_messages WHERE status = 'SENT' AND processed_at < (NOW() - CAST(:days || ' DAYS' AS INTERVAL))", nativeQuery = true)
    void deleteOldSentMessages(@Param("days") int daysOld);

    @Modifying
    @Query("UPDATE OutboxMessageEntity m SET m.status = :status, m.retryCount = :retryCount, m.processedAt = :processedAt WHERE m.id = :id")
    void update(@Param("id") Long id,
                @Param("status") OutboxStatus status,
                @Param("retryCount") int retryCount,
                @Param("processedAt") Instant processedAt);
}
