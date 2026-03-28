package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Page<SubscriptionEntity> findByChatId(Long chatId, Pageable pageable);

    List<SubscriptionEntity> findByLinkId(Long linkId);

    void deleteByChatIdAndLinkId(Long chatId, Long linkId);

    boolean existsByLinkIdAndChatId(Long linkId, Long chatId);
}
