package backend.academy.linktracker.bot.util;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TagsParser {
    public List<String> parseTags(String tagsInput) {
        if (tagsInput == null || tagsInput.trim().isEmpty()) {
            return List.of();
        }

        return List.of(tagsInput.trim().split(",")).stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}
