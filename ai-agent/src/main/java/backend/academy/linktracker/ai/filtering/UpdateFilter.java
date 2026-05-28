package backend.academy.linktracker.ai.filtering;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateFilter {
    private final AiAgentProperties properties;

    public boolean shouldProcess(RawLinkUpdateEvent event) {
        String description =
                event.getDescription() == null ? "" : event.getDescription().trim();
        String author = event.getAuthor() == null ? "" : event.getAuthor().trim();

        if (description.length() < properties.getFiltering().getMinLength()) {
            log.atInfo()
                    .addKeyValue("id", event.getId())
                    .addKeyValue("length", description.length())
                    .log("Update filtered by minimum length");
            return false;
        }

        if (isExcludedAuthor(author)) {
            log.atInfo()
                    .addKeyValue("id", event.getId())
                    .addKeyValue("author", author)
                    .log("Update filtered by excluded author");
            return false;
        }

        if (containsStopWord(description)) {
            log.atInfo().addKeyValue("id", event.getId()).log("Update filtered by stop word");
            return false;
        }

        return true;
    }

    private boolean isExcludedAuthor(String author) {
        return properties.getFiltering().getExcludedAuthors().stream()
                .anyMatch(excluded -> excluded.equalsIgnoreCase(author));
    }

    private boolean containsStopWord(String description) {
        return properties.getFiltering().getStopWords().stream()
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .map(this::stopWordPattern)
                .anyMatch(pattern -> pattern.matcher(description).find());
    }

    private Pattern stopWordPattern(String word) {
        return Pattern.compile(
                "(?<![\\p{L}\\p{N}_])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
