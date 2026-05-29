package backend.academy.linktracker.ai.grouping;

import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UpdateGroupingService {
    private final Cache<Long, UpdateGroup> groups = Caffeine.newBuilder().build();

    public void submit(ProcessedLinkUpdateEvent event) {
        event.getTgChatIds().stream().distinct().forEach(chatId -> groups.asMap()
                .compute(chatId, (key, group) -> {
                    UpdateGroup currentGroup = group == null ? new UpdateGroup(chatId, Instant.now()) : group;
                    currentGroup.add(event);
                    return currentGroup;
                }));
    }

    List<UpdateGroup> drainExpired(long windowMs) {
        Instant now = Instant.now();
        List<UpdateGroup> expiredGroups = new ArrayList<>();

        groups.asMap().forEach((chatId, group) -> {
            if (group.isExpired(now, windowMs) && groups.asMap().remove(chatId, group)) {
                expiredGroups.add(group);
            }
        });

        return expiredGroups;
    }
}
