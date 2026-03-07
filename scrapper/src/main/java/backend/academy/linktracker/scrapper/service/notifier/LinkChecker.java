package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.dto.bot.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkChecker {
    private final LinkRepository linkRepository;
    private final List<LinkTimeProvider> providers;
    private final BotNotifier botNotifier;
    private final NotificationBuilder builder;

    public void checkAllLinks() {
        linkRepository.findAll().forEach(this::checkSingleLink);

        log.atInfo().log("Link check cycle completed");
    }

    private void checkSingleLink(LinkDto link) {
        providers.stream()
                .filter(provider -> provider.supports(link.url()))
                .findFirst()
                .map(provider -> provider.getCurrentTime(link.url()))
                .filter(current -> current.isAfter(link.lastChecked()))
                .ifPresent(current -> notifyAndUpdate(link, current));
    }

    private void notifyAndUpdate(LinkDto link, Instant newTime) {
        log.atInfo().addKeyValue("id", link.id()).addKeyValue("url", link.url()).log("Link changed, notifying");

        String message = builder.buildMessage(link.url());

        botNotifier.notify(link.id(), link.url(), message, List.of(link.id()));

        linkRepository.save(link.id(), link.withLastChecked(newTime));
    }
}
