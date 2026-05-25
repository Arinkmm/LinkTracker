package backend.academy.linktracker.ai.processing;

import backend.academy.linktracker.ai.filtering.UpdateFilter;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.summarization.AiApiUpdateSummarizer;
import backend.academy.linktracker.avro.ProcessedLinkUpdateEvent;
import backend.academy.linktracker.avro.RawLinkUpdateEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateProcessingService {
    private static final String DEFAULT_PRIORITY = "HIGH"; // Пока заглушка для этого ДЗ

    private final UpdateFilter updateFilter;
    private final AiApiUpdateSummarizer updateSummarizer;
    private final AiAgentProperties properties;

    public Optional<ProcessedLinkUpdateEvent> process(RawLinkUpdateEvent event) {
        validate(event);
        if (!updateFilter.shouldProcess(event)) {
            return Optional.empty();
        }

        String processedDescription = processDescription(event.getId(), event.getDescription());

        ProcessedLinkUpdateEvent processedEvent = ProcessedLinkUpdateEvent.newBuilder()
                .setId(event.getId())
                .setUrl(event.getUrl())
                .setDescription(processedDescription)
                .setTgChatIds(event.getTgChatIds())
                .setPriority(DEFAULT_PRIORITY)
                .build();

        return Optional.of(processedEvent);
    }

    private String summarize(long id, String description) {
        log.atInfo().addKeyValue("id", id).log("Summarizing update via AI API");
        return updateSummarizer.summarize(description);
    }

    private String processDescription(long id, String description) {
        String descriptionLabel = properties.getLabels().getDescription();
        List<String> lines = description.lines().toList();
        List<String> processedLines = new ArrayList<>(lines.size());
        boolean foundBody = false;

        for (String line : lines) {
            if (line.startsWith(descriptionLabel)) {
                foundBody = true;
                String body = line.substring(descriptionLabel.length());
                processedLines.add(
                        body.length() > properties.getSummarization().getThreshold()
                                ? descriptionLabel + summarize(id, body)
                                : line);
            } else {
                processedLines.add(line);
            }
        }

        if (foundBody) {
            return String.join("\n", processedLines);
        }
        return description.length() > properties.getSummarization().getThreshold()
                ? summarize(id, description)
                : description;
    }

    private void validate(RawLinkUpdateEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("raw update event is required");
        }
        if (event.getId() == 0L) {
            throw new IllegalArgumentException("link id is required");
        }
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (event.getTgChatIds() == null || event.getTgChatIds().isEmpty()) {
            throw new IllegalArgumentException("tgChatIds must not be empty");
        }
    }
}
