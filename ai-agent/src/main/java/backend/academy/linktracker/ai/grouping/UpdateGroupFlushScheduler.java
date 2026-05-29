package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UpdateGroupFlushScheduler {
    private final AiAgentProperties properties;
    private final UpdateGroupingService groupingService;
    private final UpdateGroupPublisher publisher;

    @Scheduled(fixedDelayString = "${ai-agent.grouping.flush-interval-ms}")
    void flushExpiredGroups() {
        groupingService.drainExpired(properties.getGrouping().getWindowMs()).forEach(publisher::publish);
    }
}
