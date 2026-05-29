package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkBatchProcessor {
    private final List<LinkTimeProvider> providers;
    private final LinkUpdateProcessor linkUpdateProcessor;
    private final ScrapperMetrics metrics;

    public List<Link> processBatch(List<Link> links) {
        log.atInfo().addKeyValue("batchSize", links.size()).log("Starting batch check");

        Map<Boolean, List<Link>> partitioned = links.stream()
                .collect(Collectors.partitioningBy(l -> providers.stream().anyMatch(p -> p.supports(l.url()))));

        List<Link> failedLinks = new ArrayList<>(partitioned.get(false));
        if (!failedLinks.isEmpty()) {
            log.atWarn()
                    .addKeyValue("unsupportedCount", failedLinks.size())
                    .log("Found links with no matching provider");
        }

        Map<LinkTimeProvider, List<Link>> groupedByProvider = partitioned.get(true).stream()
                .collect(Collectors.groupingBy(l -> providers.stream()
                        .filter(p -> p.supports(l.url()))
                        .findFirst()
                        .orElseThrow()));

        groupedByProvider.forEach((provider, providerLinks) -> fetchAndProcess(provider, providerLinks, failedLinks));

        return failedLinks;
    }

    private void fetchAndProcess(LinkTimeProvider provider, List<Link> providerLinks, List<Link> failedLinks) {
        String providerName = provider.getClass().getSimpleName();
        try {
            log.atDebug()
                    .addKeyValue("provider", providerName)
                    .addKeyValue("count", providerLinks.size())
                    .log("Requesting batch from provider");

            String source = metrics.trackedSource(providerLinks.getFirst().url());
            List<LinkTimeProvider.ResponseWithLink> results = metrics.recordRequestDuration(
                    "external_source", source, () -> provider.getResponseBatch(providerLinks));

            log.atDebug()
                    .addKeyValue("provider", providerName)
                    .addKeyValue("received", results.size())
                    .log("Received provider responses");

            Set<Long> successfulIds = results.stream()
                    .filter(r -> r.linkResponse() != null)
                    .map(r -> r.link().id())
                    .collect(Collectors.toSet());

            providerLinks.stream().filter(l -> !successfulIds.contains(l.id())).forEach(link -> {
                log.atWarn()
                        .addKeyValue("linkId", link.id())
                        .addKeyValue("url", link.url())
                        .log("Provider returned no data for link");
                failedLinks.add(link);
            });

            results.forEach(res -> linkUpdateProcessor.processUpdate(res.link(), res.linkResponse()));

        } catch (Exception e) {
            log.atError().setCause(e).addKeyValue("provider", providerName).log("Failed to process batch for provider");
            failedLinks.addAll(providerLinks);
        }
    }
}
