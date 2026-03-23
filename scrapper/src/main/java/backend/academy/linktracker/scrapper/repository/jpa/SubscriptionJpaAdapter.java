package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionTagEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class SubscriptionJpaAdapter implements SubscriptionRepository {
    private final JpaSubscriptionTagRepository jpaSubscriptionTagRepository;
    private final JpaSubscriptionRepository jpaSubscriptionRepository;

    @Override
    public void save(Long userId, Long linkId, List<String> tags) {
        List<SubscriptionTagEntity> tagEntities = tags.stream()
                .map(tag -> new SubscriptionTagEntity(userId, linkId, tag))
                .toList();
        jpaSubscriptionRepository.save(new SubscriptionEntity(userId, linkId, new ArrayList<>(tagEntities)));
    }

    @Override
    public List<Subscription> findSubscriptionByUserId(Long userId, int page, int size) {
        return jpaSubscriptionRepository.findByUserId(userId, PageRequest.of(page, size)).stream()
                .map(e -> new Subscription(
                        e.getUserId(),
                        e.getLinkId(),
                        e.getTags().stream().map(SubscriptionTagEntity::getTag).toList()))
                .toList();
    }

    @Override
    public void remove(Long userId, Long linkId) {
        jpaSubscriptionRepository.deleteByUserIdAndLinkId(userId, linkId);
    }

    @Override
    public List<Long> findUserIdByLinkId(Long linkId) {
        return jpaSubscriptionRepository.findByLinkId(linkId).stream()
                .map(SubscriptionEntity::getUserId)
                .toList();
    }

    @Override
    public boolean exists(Long linkId, Long userId) {
        return jpaSubscriptionRepository.existsByLinkIdAndUserId(linkId, userId);
    }

    @Override
    public List<Long> findLinkIdByUserId(Long userId) {
        return jpaSubscriptionRepository.findByUserId(userId).stream()
                .map(SubscriptionEntity::getLinkId)
                .toList();
    }

    @Override
    public void saveTag(Long userId, Long linkId, String tag) {
        jpaSubscriptionTagRepository.save(new SubscriptionTagEntity(userId, linkId, tag));
    }

    @Override
    public void removeTag(Long userId, Long linkId, String tag) {
        jpaSubscriptionTagRepository.deleteByUserIdAndLinkIdAndTag(userId, linkId, tag);
    }

    @Override
    public List<String> findTags(Long userId, Long linkId) {
        return jpaSubscriptionTagRepository.findByUserIdAndLinkId(userId, linkId).stream()
                .map(SubscriptionTagEntity::getTag)
                .toList();
    }

    @Override
    public void updateTags(Long userId, Long linkId, List<String> tags) {
        jpaSubscriptionTagRepository.deleteByUserIdAndLinkId(userId, linkId);

        List<SubscriptionTagEntity> tagEntities = tags.stream()
                .map(tag -> new SubscriptionTagEntity(userId, linkId, tag))
                .toList();
        jpaSubscriptionTagRepository.saveAll(tagEntities);
    }
}
