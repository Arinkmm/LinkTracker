package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponses;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkUpdateProcessor {

    private final LinkService linkService;
    private final LinkNotificationService linkNotificationService;

    public void processUpdate(Link link, LinkResponse response) {
        if (response == null) {
            log.atWarn()
                .addKeyValue("linkId", link.id())
                .log("Response is null, skipping update processing for this link");
            return;
        }

        Instant latestEventDate = extractLatestEventDate(response);

        if (latestEventDate == null) {
            log.atDebug()
                .addKeyValue("linkId", link.id())
                .log("No activity dates found in response, refreshing timestamp only");
            linkService.updateLastChecked(link.id(), Instant.now());
            return;
        }

        if (link.lastChecked() == null || latestEventDate.isAfter(link.lastChecked())) {
            log.atInfo()
                .addKeyValue("linkId", link.id())
                .addKeyValue("oldDate", link.lastChecked())
                .addKeyValue("newDate", latestEventDate)
                .log("New activity detected, triggering notifications");

            linkNotificationService.updateAndNotify(link.id(), latestEventDate, response, link);
        } else {
            log.atTrace()
                .addKeyValue("linkId", link.id())
                .log("No new activity since last check");
            linkService.updateLastChecked(link.id(), Instant.now());
        }
    }

    private Instant extractLatestEventDate(LinkResponse response) {
        return switch (response) {
            case StackOverflowResponse r -> r.items().stream()
                .flatMap(item -> {
                    List<Instant> dates = new ArrayList<>();
                    dates.add(Instant.ofEpochSecond(item.lastActivityDate()));
                    if (item.answers() != null) {
                        item.answers().forEach(a -> dates.add(Instant.ofEpochSecond(a.creationDate())));
                    }
                    if (item.comments() != null) {
                        item.comments().forEach(c -> dates.add(Instant.ofEpochSecond(c.creationDate())));
                    }
                    return dates.stream();
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
