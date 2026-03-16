package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySubscriptionRepository implements SubscriptionRepository {
    private final Map<Long, Set<Long>> userToLinks = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> linkToUsers = new ConcurrentHashMap<>();

    @Override
    public void add(Long userId, Long linkId) {
        userToLinks.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(linkId);
        linkToUsers.computeIfAbsent(linkId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    @Override
    public void remove(Long userId, Long linkId) {
        Set<Long> links = userToLinks.get(userId);
        if (links != null) links.remove(linkId);

        Set<Long> users = linkToUsers.get(linkId);
        if (users != null) users.remove(linkId);
    }

    @Override
    public List<Long> findLinksByUser(Long userId) {
        return userToLinks.getOrDefault(userId, Set.of()).stream().toList();
    }

    @Override
    public List<Long> findUserIdsByLinkId(Long linkId) {
        return linkToUsers.getOrDefault(linkId, Set.of()).stream().toList();
    }

    @Override
    public void removeUser(Long userId) {
        Set<Long> linkIds = userToLinks.remove(userId);
        if (linkIds != null) {
            linkIds.forEach(lId -> {
                Set<Long> users = linkToUsers.get(lId);
                if (users != null) users.remove(userId);
            });
        }
    }

    @Override
    public boolean exists(Long userId, Long linkId) {
        Set<Long> links = userToLinks.get(userId);
        return links != null && links.contains(linkId);
    }

    @Override
    public boolean hasSubscribers(Long linkId) {
        Set<Long> users = linkToUsers.get(linkId);
        return users != null && !users.isEmpty();
    }
}
