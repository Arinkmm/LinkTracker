package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
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

    @Mock
    private SchedulerProperties schedulerProperties;

    @Mock
    private DBProperties dbProperties;

    private LinkChecker linkChecker;

    @BeforeEach
    void setUp() {
        linkChecker = new LinkChecker(
                linkRepository,
                subscriptionRepository,
                List.of(provider),
                dbProperties,
                schedulerProperties,
                botNotifier,
                notificationBuilder);
    }

    @Test
    @DisplayName("Сценарий: Время изменилось -> обновление и отправка уведомлений")
    void checkAllLinks_WhenTimeChanged_UpdatesAndNotifies() {
        Instant oldTime = Instant.now().minusSeconds(3600);
        Instant newTime = Instant.now();
        URI url = URI.create("https://github.com/user/repo");
        Long linkId = 1L;
        Long tgChatId = 12345L;

        Link link = new Link(linkId, url, oldTime);

        when(linkRepository.findStaleLinks(any(Instant.class), eq(0), eq(100))).thenReturn(List.of(link));
        when(linkRepository.findStaleLinks(any(Instant.class), eq(1), eq(100))).thenReturn(List.of());

        when(provider.supports(url)).thenReturn(true);
        when(provider.getCurrentTime(url)).thenReturn(Optional.of(newTime));
        when(subscriptionRepository.findChatIdByLinkId(linkId)).thenReturn(List.of(tgChatId));
        when(notificationBuilder.buildMessage(url)).thenReturn("Link updated!");

        linkChecker.checkAllLinks();

        verify(linkRepository).updateLastChecked(linkId, newTime);
        verify(botNotifier).notify(eq(linkId), eq(url), eq("Link updated!"), any());
    }
}
