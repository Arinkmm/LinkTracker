package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.OutboxMessageRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@RequiredArgsConstructor
public class OrmOutboxMessageRepository implements OutboxMessageRepository {
    private final JpaOutboxMessageRepository jpaOutboxMessageRepository;

    @Override
    public void save(OutboxMessageEntity outboxMessageEntity) {
        jpaOutboxMessageRepository.save(outboxMessageEntity);
    }

    @Override
    public void update(OutboxMessageEntity outboxMessageEntity) {
        jpaOutboxMessageRepository.update(outboxMessageEntity.getId(), outboxMessageEntity.getStatus(), outboxMessageEntity.getRetryCount(), outboxMessageEntity.getProcessedAt());
    }

    @Override
    public List<OutboxMessageEntity> findAll(int maxRetries, int limit) {
        return jpaOutboxMessageRepository.findAllWithLimit(maxRetries, PageRequest.of(0, limit));
    }

    @Override
    public void deleteOldSentMessages(int daysOld) {
        jpaOutboxMessageRepository.deleteOldSentMessages(daysOld);
    }
}
