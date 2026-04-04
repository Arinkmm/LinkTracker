package backend.academy.linktracker.scrapper.service.checker;

import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.properties.ThreadProperties;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkChecker {
    private final DBProperties dbProperties;
    private final SchedulerProperties schedulerProperties;
    private final LinkCheckerHelper linkCheckerHelper;
    private final ThreadProperties threadProperties;
    private final ExecutorService linkExecutorService;
    private final LinkService linkService;

    public void checkAllLinks() {
        log.info("Starting link check cycle...");
        List<Link> allFailedLinks = Collections.synchronizedList(new ArrayList<>());

        Instant threshold = Instant.now().minusMillis(schedulerProperties.getInterval());
        int page = 0;
        int size = dbProperties.getDefaultPageSize();
        List<Link> batch;

        do {
            batch = linkService.getStaleLinks(threshold, page, size);
            if (!batch.isEmpty()) {
                log.atInfo()
                        .addKeyValue("page", page)
                        .addKeyValue("batchSize", batch.size())
                        .log("Processing batch from database");

                processBatchParallel(batch, allFailedLinks);
            }
            page++;
        } while (batch.size() == size);

        if (!allFailedLinks.isEmpty()) {
            log.atWarn()
                    .addKeyValue("failedCount", allFailedLinks.size())
                    .log("Sending error notifications for failed links");
            allFailedLinks.forEach(linkCheckerHelper::notifyError);
        }

        log.atInfo()
                .addKeyValue("totalFailed", allFailedLinks.size())
                .addKeyValue("totalPages", page)
                .log("Link check cycle completed");
    }

    private void processBatchParallel(List<Link> batch, List<Link> globalFailedList) {
        int threadCount = threadProperties.getExecutedThreads();
        // Избегаем деления на ноль, если конфиг кривой
        int actualThreads = Math.max(1, threadCount);
        int partitionSize = (int) Math.ceil((double) batch.size() / actualThreads);

        List<Future<List<Link>>> futures = new ArrayList<>();

        for (int i = 0; i < batch.size(); i += partitionSize) {
            int end = Math.min(i + partitionSize, batch.size());
            List<Link> partition = new ArrayList<>(batch.subList(i, end));

            futures.add(linkExecutorService.submit(() -> linkCheckerHelper.checkBatch(partition)));

            log.atDebug()
                    .addKeyValue("partitionSize", partition.size())
                    .addKeyValue("range", i + "-" + end)
                    .log("Submitted task to executor service");
        }

        for (Future<List<Link>> future : futures) {
            try {
                List<Link> results = future.get();
                if (results != null) {
                    globalFailedList.addAll(results);
                }
            } catch (InterruptedException e) {
                log.error("Main checker thread interrupted while waiting for workers", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.atError().setCause(e.getCause()).log("Worker thread encountered a critical error");
            }
        }
    }
}
