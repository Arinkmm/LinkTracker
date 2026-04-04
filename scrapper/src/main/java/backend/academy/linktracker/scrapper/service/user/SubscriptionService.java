package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptionByLinkId(long linkId, int page, int size) {
        return subscriptionRepository.findSubscriptionByLinkId(linkId, page, size);
    }
}
