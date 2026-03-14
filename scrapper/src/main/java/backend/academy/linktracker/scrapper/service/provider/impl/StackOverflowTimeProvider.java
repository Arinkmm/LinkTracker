package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StackOverflowTimeProvider implements LinkTimeProvider {
    private final StackOverflowClient client;

    @Override
    public boolean supports(URI url) {
        String host = url.getHost();
        String path = url.getPath();

        return host != null && host.contains("stackoverflow.com") && path != null && path.contains("/questions/");
    }

    @Override
    public Instant getCurrentTime(URI url) {
        try {
            String questionId = extractQuestionId(url);
            if (questionId == null) {
                return Instant.EPOCH;
            }

            log.atDebug().addKeyValue("question_id", questionId).log("Fetching StackOverflow last activity");

            return client.getQuestion(questionId).lastActivity();
        } catch (Exception e) {
            log.atWarn()
                    .addKeyValue("url", url)
                    .addKeyValue("error", e.getClass().getSimpleName())
                    .log("Failed to get StackOverflow time");

            return Instant.EPOCH;
        }
    }

    private String extractQuestionId(URI url) {
        String path = url.getPath();
        if (path == null || !path.contains("/questions/")) {
            return null;
        }

        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("questions") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
