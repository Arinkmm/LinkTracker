package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponses;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.builder.NotificationBuilder;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import backend.academy.linktracker.scrapper.service.user.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkCheckerHelper {
    private final List<LinkTimeProvider> providers;
    private final DBProperties dbProperties;
    private final BotNotifier botNotifier;
    private final NotificationBuilder builder;
    private final LinkService linkService;
    private final SubscriptionService subscriptionService;

    public List<Link> checkBatch(List<Link> links) {
        log.atInfo()
            .addKeyValue("batchSize", links.size())
            .log("Starting batch check");

        Map<Boolean, List<Link>> partitioned = links.stream()
            .collect(Collectors.partitioningBy(l -> providers.stream().anyMatch(p -> p.supports(l.url()))));

        List<Link> failedLinks = new ArrayList<>(partitioned.get(false));
        if (!failedLinks.isEmpty()) {
            log.atWarn()
                .addKeyValue("unsupportedCount", failedLinks.size())
                .log("Found links with no matching provider");
        }

        List<Link> supportedLinks = partitioned.get(true);
        Map<LinkTimeProvider, List<Link>> grouped = supportedLinks.stream()
            .collect(Collectors.groupingBy(l -> providers.stream()
                .filter(p -> p.supports(l.url())).findFirst().get()));

        grouped.forEach((provider, providerList) -> {
            String providerName = provider.getClass().getSimpleName();
            try {
                log.atDebug()
                    .addKeyValue("provider", providerName)
                    .addKeyValue("count", providerList.size())
                    .log("Requesting batch from provider");

                List<LinkTimeProvider.ResponseWithLink> results = provider.getResponseBatch(providerList);

                log.atDebug()
                    .addKeyValue("provider", providerName)
                    .addKeyValue("received", results.size())
                    .log("Received provider responses");

                Set<Long> successfulIds = results.stream()
                    .map(r -> r.link().id())
                    .collect(Collectors.toSet());

                providerList.stream()
                    .filter(l -> !successfulIds.contains(l.id()))
                    .forEach(link -> {
                        log.atWarn()
                            .addKeyValue("linkId", link.id())
                            .addKeyValue("url", link.url())
                            .log("Provider returned no data for link");
                        failedLinks.add(link);
                    });

                results.forEach(res -> processSingleUpdate(res.link(), res.linkResponse()));

            } catch (Exception e) {
                log.atError()
                    .setCause(e)
                    .addKeyValue("provider", providerName)
                    .log("Failed to process batch for provider");
                failedLinks.addAll(providerList);
            }
        });
        return failedLinks;
    }

    private void processSingleUpdate(Link link, LinkResponse response) {
        Instant newEventDate = extractDate(response);

        if (response == null) {
            log.warn("Response is null, skipping update processing for this link");
            return;
        }

        if (newEventDate == null) {
            log.atDebug()
                .addKeyValue("linkId", link.id())
                .log("No activity dates extracted from response, skipping update");
            updateLinkTime(link.id(), Instant.now());
            return;
        }

        if (link.lastChecked() == null || newEventDate.isAfter(link.lastChecked())) {
            log.atInfo()
                .addKeyValue("linkId", link.id())
                .addKeyValue("oldDate", link.lastChecked())
                .addKeyValue("newDate", newEventDate)
                .log("New activity detected, triggering notifications");

            sendNotifications(link, response);
            updateLinkTime(link.id(), newEventDate);
        } else {
            log.atTrace()
                .addKeyValue("linkId", link.id())
                .log("No new activity since last check");
            updateLinkTime(link.id(), Instant.now());
        }
    }

    public void sendNotifications(Link link, LinkResponse response) {
        log.atDebug().addKeyValue("linkId", link.id()).log("Building and sending notification");
        String message = builder.buildMessage(response);
        processBotNotification(link, message);
    }

    public void notifyError(Link link) {
        log.atWarn().addKeyValue("linkId", link.id()).log("Sending error notification to subscribers");
        String message = builder.buildError(link);
        processBotNotification(link, message);
    }

    private void processBotNotification(Link link, String message) {
        int page = 0;
        int size = dbProperties.getDefaultPageSize();
        List<Subscription> batch;
        int totalNotified = 0;

        do {
            batch = subscriptionService.getSubscriptionByLinkId(link.id(), page, size);
            List<Long> chatIds = batch.stream().map(Subscription::chatId).toList();
            if (!chatIds.isEmpty()) {
                botNotifier.notify(link.id(), link.url(), message, chatIds);
                totalNotified += chatIds.size();
            }
            page++;
        } while (batch.size() == size);

        log.atDebug()
            .addKeyValue("linkId", link.id())
            .addKeyValue("subscriberCount", totalNotified)
            .log("Bot notifications processed");
    }

    public void updateLinkTime(Long id, Instant time) {
        linkService.updateLastChecked(id, time);
    }

    private Instant extractDate(LinkResponse response) {
        if (response == null) {
            return null;
        }
        return switch (response) {
            case StackOverflowResponse r -> r.items().stream()
                .flatMap(item -> {
                    List<Instant> allDates = new ArrayList<>();
                    allDates.add(Instant.ofEpochSecond(item.lastActivityDate()));

                    if (item.answers() != null) {
                        item.answers().forEach(a -> allDates.add(Instant.ofEpochSecond(a.creationDate())));
                    }
                    if (item.comments() != null) {
                        item.comments().forEach(c -> allDates.add(Instant.ofEpochSecond(c.creationDate())));
                    }
                    return allDates.stream();
                })
                .max(Instant::compareTo)
                .orElse(null);

            case GitHubRepoResponses r -> r.issues().stream()
                .map(GitHubRepoResponse::createdAt)
                .max(Instant::compareTo)
                .orElse(null);

            default -> null;
        };
    }
}
