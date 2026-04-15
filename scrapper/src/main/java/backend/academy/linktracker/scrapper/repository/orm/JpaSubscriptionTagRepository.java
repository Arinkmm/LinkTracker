package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionTagEntity;
import backend.academy.linktracker.scrapper.repository.orm.id.SubscriptionTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubscriptionTagRepository extends JpaRepository<SubscriptionTagEntity, SubscriptionTagId> {
    List<SubscriptionTagEntity> findBySubscriptionId(Long subscriptionId);

    void deleteBySubscriptionIdAndTag(Long subscriptionId, String tag);
}
