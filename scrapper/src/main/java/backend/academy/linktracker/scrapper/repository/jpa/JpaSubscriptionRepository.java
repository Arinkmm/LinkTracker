package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.repository.jpa.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.jpa.id.SubscriptionId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, SubscriptionId> {
    Page<SubscriptionEntity> findByUserId(Long userId, Pageable pageable);

    List<SubscriptionEntity> findByUserId(Long userId);

    List<SubscriptionEntity> findByLinkId(Long linkId);

    void deleteByUserIdAndLinkId(Long userId, Long linkId);

    boolean existsByLinkIdAndUserId(Long linkId, Long userId);
}
