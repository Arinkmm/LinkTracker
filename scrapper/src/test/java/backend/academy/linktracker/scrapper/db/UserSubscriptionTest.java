package backend.academy.linktracker.scrapper.db;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import backend.academy.linktracker.scrapper.configuration.CacheIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserSubscriptionTest extends CacheIntegrationEnvironment {
    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("Повторная подписка выбрасывает LinkAlreadyTrackedException")
    void shouldHandleDuplicateSubscription() {
        Long chatId = 1L;
        chatRepository.save(chatId);
        AddLinkRequest request = new AddLinkRequest();
        request.setLink(URI.create("https://example.com"));

        linkService.addLink(chatId, request);

        assertThatThrownBy(() -> linkService.addLink(chatId, request)).isInstanceOf(LinkAlreadyTrackedException.class);
    }

    @Test
    @DisplayName("Подписка: создание Link, Chat и Tags в БД")
    void subscribe_ShouldPersistAllEntities() {
        Long chatId = 1001L;
        chatRepository.save(chatId);
        URI url = URI.create("https://github.com/owner/repo");
        List<String> tags = List.of("java", "spring");

        AddLinkRequest request = new AddLinkRequest();
        request.setLink(url);
        request.setTags(tags);
        request.setFilters(List.of());

        linkService.addLink(chatId, request);

        assertTrue(linkRepository.findByUrl(url).isPresent(), "Ссылка должна быть в БД");

        Long linkId = linkRepository.findByUrl(url).get().id();
        assertTrue(subscriptionRepository.exists(linkId, chatId), "Связь подписки должна существовать");

        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionByChatId(chatId, 0, 10);
        assertTrue(subscriptions.getFirst().tags().containsAll(tags), "Теги должны быть сохранены");
    }
}
