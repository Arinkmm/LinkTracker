package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.notifier.LinkNotifier;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
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
    private final LinkNotifier linkNotifier;

    public void checkAllLinks() {
        linkRepository.findAll().forEach(this::checkSingleLink);
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
        String message = linkNotifier.buildMessage(link.url());

        botNotifier.notify(
            link.userId(), link.url(), message, List.of(link.userId())
        );

        linkRepository.save(link.userId(),
            link.withLastChecked(newTime));
    }
}
