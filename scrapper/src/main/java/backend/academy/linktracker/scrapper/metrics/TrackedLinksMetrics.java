package backend.academy.linktracker.scrapper.metrics;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrackedLinksMetrics {
    private static final String LINKS_ON_TRACK_METRIC = "links_on_track";
    private static final String TAG_TRACKED_SOURCE = "tracked_source";

    private final LinkRepository linkRepository;
    private final ScrapperMetrics metrics;
    private final AtomicLong githubLinks = new AtomicLong();
    private final AtomicLong stackoverflowLinks = new AtomicLong();
    private final AtomicLong otherLinks = new AtomicLong();

    public TrackedLinksMetrics(MeterRegistry meterRegistry, LinkRepository linkRepository, ScrapperMetrics metrics) {
        this.linkRepository = linkRepository;
        this.metrics = metrics;
        registerGauge(meterRegistry, ScrapperMetrics.SOURCE_GITHUB, githubLinks);
        registerGauge(meterRegistry, ScrapperMetrics.SOURCE_STACKOVERFLOW, stackoverflowLinks);
        registerGauge(meterRegistry, ScrapperMetrics.SOURCE_OTHER, otherLinks);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${management.metrics.tracked-links-refresh-interval-ms}")
    public void refresh() {
        try {
            Map<String, Long> counts = metrics.recordRequestDuration(
                    ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_LINKS, linkRepository::countBySource);

            githubLinks.set(counts.getOrDefault(ScrapperMetrics.SOURCE_GITHUB, 0L));
            stackoverflowLinks.set(counts.getOrDefault(ScrapperMetrics.SOURCE_STACKOVERFLOW, 0L));
            otherLinks.set(counts.getOrDefault(ScrapperMetrics.SOURCE_OTHER, 0L));
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Failed to refresh tracked links metrics");
        }
    }

    private void registerGauge(MeterRegistry meterRegistry, String source, AtomicLong value) {
        Gauge.builder(LINKS_ON_TRACK_METRIC, value, AtomicLong::get)
                .tag(TAG_TRACKED_SOURCE, source)
                .register(meterRegistry);
    }
}
