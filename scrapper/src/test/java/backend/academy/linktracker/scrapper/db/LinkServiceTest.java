package backend.academy.linktracker.scrapper.db;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import backend.academy.linktracker.scrapper.configuration.DatabaseIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class LinkServiceTest extends DatabaseIntegrationEnvironment {
    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatRepository chatRepository;

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
}
