package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import java.util.List;

public interface OutboxMessageRepository {
    void save(OutboxMessageEntity outboxMessageEntity);
    void update(OutboxMessageEntity entity);
    List<OutboxMessageEntity> findAll(int maxRetries, int limit);
    void deleteOldSentMessages(int daysOld);
}
