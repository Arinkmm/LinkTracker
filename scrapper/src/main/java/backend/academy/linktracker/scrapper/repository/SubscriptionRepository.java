package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.Subscription;
import java.util.List;

public interface SubscriptionRepository {
    void save(Long chatId, Long linkId, List<String> tags);

    void remove(Long chatId, Long linkId);

    List<Long> findChatIdByLinkId(Long linkId);

    boolean exists(Long linkId, Long chatId);

    List<Subscription> findSubscriptionByChatId(Long chatId, int page, int size);

    void saveTag(Long subscriptionId, String tag);

    void removeTag(Long subscriptionId, String tag);

    List<String> findTags(Long subscriptionId);

    void updateTags(Long subscriptionId, List<String> tags);
}
