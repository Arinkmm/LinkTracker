package backend.academy.linktracker.ai.processing;

import backend.academy.linktracker.ai.exception.AiSummarizationException;
import backend.academy.linktracker.ai.filtering.UpdateFilter;
import backend.academy.linktracker.ai.prioritization.UpdatePrioritizer;
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
    private final UpdateFilter updateFilter;
    private final UpdatePrioritizer prioritizer;
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
                .setPriority(prioritizer.prioritize(event.getDescription()).name())
                .build();

        return Optional.of(processedEvent);
    }

    private String summarize(long id, String description) {
        log.atInfo().addKeyValue("id", id).log("Summarizing update via AI API");
        try {
            return updateSummarizer.summarize(description);
        } catch (AiSummarizationException e) {
            log.atWarn()
                    .setCause(e)
                    .addKeyValue("id", id)
                    .log("AI API summarization failed, using fallback truncation");
            return fallbackSummary(description);
        }
    }

    private String processDescription(long id, String description) {
        String descriptionLabel = properties.getLabels().getDescription();
        List<String> lines = description.lines().toList();
        List<String> processedLines = new ArrayList<>(lines.size());
        boolean foundBody = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(descriptionLabel)) {
                foundBody = true;
                int bodyEnd = findBodyEnd(lines, i + 1);
                String body = extractBody(descriptionLabel, lines, i, bodyEnd);

                if (shouldSummarize(body)) {
                    processedLines.add(descriptionLabel + summarize(id, body));
                } else {
                    processedLines.addAll(lines.subList(i, bodyEnd));
                }
                i = bodyEnd - 1;
            } else {
                processedLines.add(line);
            }
        }

        if (foundBody) {
            return String.join("\n", processedLines);
        }
        return shouldSummarize(description) ? summarize(id, description) : description;
    }

    private int findBodyEnd(List<String> lines, int start) {
        int end = start;
        while (end < lines.size() && !isSeparatorLine(lines.get(end))) {
            end++;
        }
        return end;
    }

    private String extractBody(String descriptionLabel, List<String> lines, int labelIndex, int bodyEnd) {
        List<String> bodyLines = new ArrayList<>();
        String firstLine = lines.get(labelIndex).substring(descriptionLabel.length());
        if (!firstLine.isBlank()) {
            bodyLines.add(firstLine);
        }
        bodyLines.addAll(lines.subList(labelIndex + 1, bodyEnd));
        return String.join("\n", bodyLines).trim();
    }

    private boolean shouldSummarize(String description) {
        return description.length() > properties.getSummarization().getThreshold();
    }

    private String fallbackSummary(String description) {
        int threshold = properties.getSummarization().getThreshold();
        if (description.length() <= threshold) {
            return description;
        }
        if (threshold <= 3) {
            return description.substring(0, threshold);
        }
        return description.substring(0, threshold - 3) + "...";
    }

    private boolean isSeparatorLine(String line) {
        String divider = properties.getLabels().getDivider();
        return line.trim().equals(divider);
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
