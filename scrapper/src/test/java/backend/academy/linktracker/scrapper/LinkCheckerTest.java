package backend.academy.linktracker.scrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.LinkChecker;
import backend.academy.linktracker.scrapper.service.notifier.NotificationBuilder;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LinkCheckerTest {
    @Mock
    private LinkRepository linkRepository;

    @Mock
    private BotNotifier botNotifier;

    @Mock
    private NotificationBuilder notificationBuilder;

    @Spy
    private List<LinkTimeProvider> providers = List.of(mock(LinkTimeProvider.class));

    @InjectMocks
    private LinkChecker linkChecker;

    @Test
    @DisplayName("Сценарий: Обновление отправляется только пользователям с изменившейся ссылкой")
    void checkAllLinks_SendsUpdatesOnlyToAffectedUsers() {
        Instant now = Instant.now();
        Instant oldTime = now.minusSeconds(1000);
        Instant newTime = now.plusSeconds(1000);

        LinkDto linkWithUpdate =
                new LinkDto(10L, URI.create("https://github.com/user/updated"), List.of(), List.of(), oldTime);

        LinkDto linkWithoutUpdate =
                new LinkDto(20L, URI.create("https://github.com/user/stable"), List.of(), List.of(), oldTime);

        when(linkRepository.findAll()).thenReturn(List.of(linkWithUpdate, linkWithoutUpdate));

        LinkTimeProvider provider = providers.get(0);

        when(provider.supports(linkWithUpdate.url())).thenReturn(true);
        when(provider.getCurrentTime(linkWithUpdate.url())).thenReturn(newTime);

        when(provider.supports(linkWithoutUpdate.url())).thenReturn(true);
        when(provider.getCurrentTime(linkWithoutUpdate.url())).thenReturn(oldTime);

        when(notificationBuilder.buildMessage(any(URI.class))).thenReturn("New Update!");

        linkChecker.checkAllLinks();

        verify(botNotifier).notify(eq(10L), eq(linkWithUpdate.url()), anyString(), anyList());
        verify(linkRepository).save(eq(10L), argThat(link -> link.lastChecked().equals(newTime)));

        verify(botNotifier, never()).notify(eq(20L), any(), anyString(), any());
        verify(linkRepository, never()).save(eq(20L), any());
    }
}
