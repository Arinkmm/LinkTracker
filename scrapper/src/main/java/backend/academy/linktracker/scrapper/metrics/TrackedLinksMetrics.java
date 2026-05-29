package backend.academy.linktracker.scrapper.metrics;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrackedLinksMetrics {
    private final LinkRepository linkRepository;
    private final ScrapperMetrics metrics;
    private final AtomicLong githubLinks = new AtomicLong();
    private final AtomicLong stackoverflowLinks = new AtomicLong();
    private final AtomicLong otherLinks = new AtomicLong();

    public TrackedLinksMetrics(MeterRegistry meterRegistry, LinkRepository linkRepository, ScrapperMetrics metrics) {
        this.linkRepository = linkRepository;
        this.metrics = metrics;
        registerGauge(meterRegistry, "github", githubLinks);
        registerGauge(meterRegistry, "stackoverflow", stackoverflowLinks);
        registerGauge(meterRegistry, "other", otherLinks);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${management.metrics.tracked-links-refresh-interval-ms}")
    public void refresh() {
        try {
            List<Link> links = metrics.recordRequestDuration("database", "links", linkRepository::findAll);
            Map<String, Long> counts = links.stream()
                    .collect(Collectors.groupingBy(link -> metrics.trackedSource(link.url()), Collectors.counting()));

            githubLinks.set(counts.getOrDefault("github", 0L));
            stackoverflowLinks.set(counts.getOrDefault("stackoverflow", 0L));
            otherLinks.set(counts.getOrDefault("other", 0L));
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Failed to refresh tracked links metrics");
        }
    }

    private void registerGauge(MeterRegistry meterRegistry, String source, AtomicLong value) {
        Gauge.builder("links_on_track", value, AtomicLong::get)
                .tag("tracked_source", source)
                .register(meterRegistry);
    }
}
