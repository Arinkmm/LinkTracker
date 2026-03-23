package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LinkChecker {
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final List<LinkTimeProvider> providers;
    private final SchedulerProperties schedulerProperties;
    private final BotNotifier botNotifier;
    private final NotificationBuilder builder;

    public void checkAllLinks() {
        Instant threshold = Instant.now().minusSeconds(schedulerProperties.getInterval() / 1000);
        int page = 0;
        int size = 100;
        List<Link> batch;

        do {
            batch = linkRepository.findStaleLinks(threshold, page, size);
            batch.forEach(this::checkSingleLink);
            page++;
        } while (batch.size() == size);

        log.atInfo().log("Link check cycle completed");
    }

    private void checkSingleLink(Link link) {
        providers.stream()
                .filter(provider -> provider.supports(link.url()))
                .findFirst()
                .flatMap(provider -> provider.getCurrentTime(link.url()))
                .ifPresent(current -> {
                    if (link.lastChecked() == null) {
                        linkRepository.updateLastChecked(link.id(), current);
                    } else if (current.isAfter(link.lastChecked())) {
                        notifyAndUpdate(link, current);
                    }
                });
    }

    private void notifyAndUpdate(Link link, Instant newTime) {
        log.atInfo().addKeyValue("id", link.id()).addKeyValue("url", link.url()).log("Link changed, notifying");

        String message = builder.buildMessage(link.url());

        List<Long> tgChatIds = subscriptionRepository.findUserIdByLinkId(link.id());

        if (!tgChatIds.isEmpty()) {
            botNotifier.notify(link.id(), link.url(), message, tgChatIds);
        }

        linkRepository.updateLastChecked(link.id(), newTime);
    }
}
