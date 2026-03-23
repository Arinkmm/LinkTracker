package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.Subscription;
import java.util.List;

public interface SubscriptionRepository {
    void save(Long userId, Long linkId, List<String> tags);

    void remove(Long userId, Long linkId);

    List<Long> findUserIdByLinkId(Long linkId);

    boolean exists(Long linkId, Long userId);

    List<Subscription> findSubscriptionByUserId(Long userId, int page, int size);

    List<Long> findLinkIdByUserId(Long userId);

    void saveTag(Long userId, Long linkId, String tag);

    void removeTag(Long userId, Long linkId, String tag);

    List<String> findTags(Long userId, Long linkId);

    void updateTags(Long userId, Long linkId, List<String> tags);
}
