package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Page<SubscriptionEntity> findByChatId(Long chatId, Pageable pageable);

    @Query("SELECT s FROM SubscriptionEntity s WHERE s.linkId = :linkId")
    Page<SubscriptionEntity> findByLinkId(Long linkId, Pageable pageable);

    void deleteByChatIdAndLinkId(Long chatId, Long linkId);

    boolean existsByLinkIdAndChatId(Long linkId, Long chatId);
}
