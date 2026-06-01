package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final ScrapperMetrics metrics;

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionByLinkId(long linkId, int page, int size) {
        return metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE,
                ScrapperMetrics.TYPE_SUBSCRIPTIONS,
                () -> subscriptionRepository.findSubscriptionByLinkId(linkId, page, size));
    }
}
