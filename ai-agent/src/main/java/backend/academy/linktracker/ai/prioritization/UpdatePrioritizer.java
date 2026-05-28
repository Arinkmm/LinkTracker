package backend.academy.linktracker.ai.prioritization;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdatePrioritizer {
    private final AiAgentProperties properties;

    public UpdatePriority prioritize(String text) {
        if (containsKeyword(text, properties.getPrioritization().getHighKeywords())) {
            return UpdatePriority.HIGH;
        }
        if (containsKeyword(text, properties.getPrioritization().getLowKeywords())) {
            return UpdatePriority.LOW;
        }
        return UpdatePriority.MEDIUM;
    }

    private boolean containsKeyword(String text, List<String> keywords) {
        String value = text == null ? "" : text;
        return keywords.stream()
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .map(this::keywordPattern)
                .anyMatch(pattern -> pattern.matcher(value).find());
    }

    private Pattern keywordPattern(String keyword) {
        String boundaryPattern = "(?<![\\p{L}\\p{N}_])" + Pattern.quote(keyword) + "(?![\\p{L}\\p{N}_])";
        return Pattern.compile(boundaryPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
