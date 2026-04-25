package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.service.builder.impl.NotificationBuilderService;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import backend.academy.linktracker.scrapper.service.user.SubscriptionService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkNotificationService {

    private final BotNotifier botNotifier;
    private final LinkService linkService;
    private final NotificationBuilderService notificationBuilder;
    private final DBProperties dbProperties;
    private final SubscriptionService subscriptionService;

    @Transactional
    public void updateAndNotify(Long linkId, Instant newEventDate, LinkResponse response, Link link) {
        linkService.updateLastChecked(linkId, newEventDate);
        String message = notificationBuilder.buildMessage(response);
        dispatchToSubscribers(link, message);
    }

    @Transactional
    public void notifyError(Link link) {
        log.atWarn().addKeyValue("linkId", link.id()).log("Sending error notification to subscribers");
        String message = notificationBuilder.buildError(link);
        dispatchToSubscribers(link, message);
    }

    private void dispatchToSubscribers(Link link, String message) {
        int page = 0;
        int size = dbProperties.getDefaultPageSize();
        int totalNotified = 0;
        List<Subscription> batch;

        do {
            batch = subscriptionService.getSubscriptionByLinkId(link.id(), page, size);
            List<Long> chatIds = batch.stream().map(Subscription::chatId).toList();

            if (!chatIds.isEmpty()) {
                LinkUpdate linkUpdate = new LinkUpdate();
                linkUpdate.setId(link.id());
                linkUpdate.setUrl(link.url());
                linkUpdate.setDescription(message);
                linkUpdate.setTgChatIds(chatIds);

                botNotifier.notify(linkUpdate);
                totalNotified += chatIds.size();
            }
            page++;
        } while (batch.size() == size);

        log.atDebug()
            .addKeyValue("linkId", link.id())
            .addKeyValue("subscriberCount", totalNotified)
            .log("Notifications queued in outbox");
    }
}
