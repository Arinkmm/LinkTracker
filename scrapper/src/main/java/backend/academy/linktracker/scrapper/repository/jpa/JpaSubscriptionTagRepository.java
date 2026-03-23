package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.jpa.id.SubscriptionTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubscriptionTagRepository extends JpaRepository<SubscriptionTagEntity, SubscriptionTagId> {
    List<SubscriptionTagEntity> findByUserIdAndLinkId(Long userId, Long linkId);

    void deleteByUserIdAndLinkId(Long userId, Long linkId);

    void deleteByUserIdAndLinkIdAndTag(Long userId, Long linkId, String tag);
}
