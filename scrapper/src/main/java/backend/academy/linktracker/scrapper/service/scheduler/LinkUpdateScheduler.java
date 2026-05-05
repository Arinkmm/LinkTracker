package backend.academy.linktracker.scrapper.service.scheduler;

import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUpdateScheduler {
    private final LinkChecker linkChecker;

    @Scheduled(fixedDelayString = "${scheduler.interval-ms}")
    public void checkUpdates() {
        log.atInfo().log("Starting scheduled link check");

        linkChecker.checkAllLinks();
    }
}
