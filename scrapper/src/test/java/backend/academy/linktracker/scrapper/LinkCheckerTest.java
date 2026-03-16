package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.LinkChecker;
import backend.academy.linktracker.scrapper.service.notifier.NotificationBuilder;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinkCheckerTest {
    @Mock
    private LinkRepository linkRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private BotNotifier botNotifier;

    @Mock
    private NotificationBuilder notificationBuilder;

    @Mock
    private LinkTimeProvider provider;

    private LinkChecker linkChecker;

    @BeforeEach
    void setUp() {
        linkChecker = new LinkChecker(
                linkRepository, subscriptionRepository, List.of(provider), botNotifier, notificationBuilder);
    }

    @Test
    @DisplayName("Сценарий: Время изменилось -> обновление и отправка уведомлений")
    void checkAllLinks_WhenTimeChanged_UpdatesAndNotifies() {
        Instant oldTime = Instant.now().minusSeconds(3600);
        Instant newTime = Instant.now();
        URI url = URI.create("https://github.com/user/repo");
        Long linkId = 1L;
        Long tgChatId = 12345L;

        LinkDto link = new LinkDto(linkId, url, null, null, oldTime);

        when(linkRepository.findAll()).thenReturn(List.of(link));

        when(provider.supports(url)).thenReturn(true);
        when(provider.getCurrentTime(url)).thenReturn(Optional.of(newTime));

        when(subscriptionRepository.findUserIdsByLinkId(linkId)).thenReturn(List.of(tgChatId));
        when(notificationBuilder.buildMessage(url)).thenReturn("Link updated!");

        linkChecker.checkAllLinks();

        verify(linkRepository).updateLastChecked(linkId, newTime);

        verify(botNotifier).notify(eq(tgChatId), eq(url), eq("Link updated!"), any());
    }
}
