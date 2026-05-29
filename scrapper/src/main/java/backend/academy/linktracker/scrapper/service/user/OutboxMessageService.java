package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.repository.OutboxMessageRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxMessageEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.OutboxStatus;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxMessageService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ScrapperMetrics metrics;

    @Transactional
    public void addMessage(OutboxMessageEntity entity) {
        metrics.recordRequestDuration("database", "outbox_messages", () -> outboxMessageRepository.save(entity));
    }

    @Transactional
    public List<OutboxMessageEntity> getOutboxMessages(int maxRetries, int limit) {
        return metrics.recordRequestDuration(
                "database", "outbox_messages", () -> outboxMessageRepository.findAll(maxRetries, limit));
    }

    @Transactional
    public void markAsSent(OutboxMessageEntity message) {
        message.setStatus(OutboxStatus.SENT);
        message.setUpdatedAt(Instant.now());
        metrics.recordRequestDuration("database", "outbox_messages", () -> outboxMessageRepository.update(message));
    }

    @Transactional
    public void markAsError(OutboxMessageEntity message) {
        message.setStatus(OutboxStatus.ERROR);
        message.setRetryCount(message.getRetryCount() + 1);
        message.setUpdatedAt(Instant.now());
        metrics.recordRequestDuration("database", "outbox_messages", () -> outboxMessageRepository.update(message));
    }

    @Transactional
    public void deleteOldSentMessages(int days) {
        metrics.recordRequestDuration(
                "database", "outbox_messages", () -> outboxMessageRepository.deleteOldSentMessages(days));
    }
}
