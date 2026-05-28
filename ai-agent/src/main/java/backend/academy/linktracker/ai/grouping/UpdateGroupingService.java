package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UpdateGroupingService {
    private final Cache<List<Long>, UpdateGroup> groups = Caffeine.newBuilder().build();

    public void submit(ProcessedLinkUpdateEvent event) {
        List<Long> chatIds = chatIdsKey(event);
        groups.asMap().compute(chatIds, (key, group) -> {
            UpdateGroup currentGroup = group == null ? new UpdateGroup(Instant.now()) : group;
            return currentGroup.add(event);
        });
    }

    List<UpdateGroup> drainExpired(long windowMs) {
        Instant now = Instant.now();
        List<UpdateGroup> expiredGroups = new ArrayList<>();

        groups.asMap().forEach((chatIds, group) -> {
            if (group.isExpired(now, windowMs) && groups.asMap().remove(chatIds, group)) {
                expiredGroups.add(group);
            }
        });

        return expiredGroups;
    }

    private List<Long> chatIdsKey(ProcessedLinkUpdateEvent event) {
        return event.getTgChatIds().stream().sorted(Comparator.naturalOrder()).toList();
    }
}
