package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class OrmSubscriptionRepository implements SubscriptionRepository {

    private final JpaSubscriptionTagRepository jpaSubscriptionTagRepository;
    private final JpaSubscriptionRepository jpaSubscriptionRepository;

    @Override
    public void save(Long chatId, Long linkId, List<String> tags) {
        SubscriptionEntity entity = new SubscriptionEntity(chatId, linkId);
        SubscriptionEntity saved = jpaSubscriptionRepository.save(entity);

        tags.forEach(saved::addTag);
        jpaSubscriptionRepository.save(saved);
    }

    @Override
    public List<Subscription> findSubscriptionByChatId(Long chatId, int page, int size) {
        return jpaSubscriptionRepository.findByChatId(chatId, PageRequest.of(page, size)).stream()
                .map(e -> new Subscription(
                        e.getChatId(),
                        e.getLinkId(),
                        e.getTags().stream().map(SubscriptionTagEntity::getTag).toList()))
                .toList();
    }

    @Override
    public List<Subscription> findSubscriptionByLinkId(Long linkId, int page, int size) {
        return jpaSubscriptionRepository.findByLinkId(linkId, PageRequest.of(page, size)).stream()
                .map(e -> new Subscription(
                        e.getChatId(),
                        e.getLinkId(),
                        e.getTags().stream().map(SubscriptionTagEntity::getTag).toList()))
                .toList();
    }

    @Override
    public void remove(Long chatId, Long linkId) {
        jpaSubscriptionRepository.deleteByChatIdAndLinkId(chatId, linkId);
    }

    @Override
    public boolean exists(Long linkId, Long chatId) {
        return jpaSubscriptionRepository.existsByLinkIdAndChatId(linkId, chatId);
    }

    @Override
    public void saveTag(Long subscriptionId, String tag) {
        jpaSubscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
            sub.addTag(tag);
            jpaSubscriptionRepository.save(sub);
        });
    }

    @Override
    public void removeTag(Long subscriptionId, String tag) {
        jpaSubscriptionTagRepository.deleteBySubscriptionIdAndTag(subscriptionId, tag);
    }

    @Override
    public List<String> findTags(Long subscriptionId) {
        return jpaSubscriptionTagRepository.findBySubscriptionId(subscriptionId).stream()
                .map(SubscriptionTagEntity::getTag)
                .toList();
    }

    @Override
    public void updateTags(Long subscriptionId, List<String> tags) {
        jpaSubscriptionRepository.findById(subscriptionId).ifPresent(sub -> {
            sub.getTags().clear();
            tags.forEach(sub::addTag);
            jpaSubscriptionRepository.save(sub);
        });
    }
}
