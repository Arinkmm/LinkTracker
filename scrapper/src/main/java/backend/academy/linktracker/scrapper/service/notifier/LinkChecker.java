package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.properties.DBProperties;
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
@RequiredArgsConstructor
public class LinkChecker {
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final List<LinkTimeProvider> providers;
    private final DBProperties dbProperties;
    private final SchedulerProperties schedulerProperties;
    private final BotNotifier botNotifier;
    private final NotificationBuilder builder;

    public void checkAllLinks() {
        Instant threshold = Instant.now().minusSeconds(schedulerProperties.getInterval() / 1000);
        int page = 0;
        int size = dbProperties.getDefaultPageSize();
        List<Link> batch;

        do {
            batch = linkRepository.findStaleLinks(threshold, page, size);
            batch.forEach(this::checkSingleLink);
            page++;
        } while (batch.size() == size);

        log.atInfo().log("Link check cycle completed");
    }

    @Transactional
    public void checkSingleLink(Link link) {
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

    @Transactional
    public void notifyAndUpdate(Link link, Instant newTime) {
        log.atInfo().addKeyValue("id", link.id()).addKeyValue("url", link.url()).log("Link changed, notifying");

        String message = builder.buildMessage(link.url());

        int page = 0;
        int size = dbProperties.getDefaultPageSize();
        List<Subscription> batch;

        do {
            batch = subscriptionRepository.findSubscriptionByChatId(link.id(), page, size);
            List<Long> tgChatIds = batch.stream().map(Subscription::chatId).toList();
            if (!tgChatIds.isEmpty()) {
                botNotifier.notify(link.id(), link.url(), message, tgChatIds);
            }
            page++;
        } while (batch.size() == size);

        linkRepository.updateLastChecked(link.id(), newTime);
    }
}
