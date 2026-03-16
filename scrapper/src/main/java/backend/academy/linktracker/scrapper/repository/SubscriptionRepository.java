package backend.academy.linktracker.scrapper.repository;

import java.util.List;

public interface SubscriptionRepository {
    void add(Long userId, Long linkId);

    void remove(Long userId, Long linkId);

    List<Long> findLinksByUser(Long userId);

    List<Long> findUserIdsByLinkId(Long linkId);

    void removeUser(Long userId);

    boolean exists(Long userId, Long linkId);

    boolean hasSubscribers(Long linkId);
}
