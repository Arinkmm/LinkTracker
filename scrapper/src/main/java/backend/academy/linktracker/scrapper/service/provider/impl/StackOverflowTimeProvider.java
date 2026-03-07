package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
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
    public boolean supports(String url) {
        return url.contains("stackoverflow.com/questions/");
    }

    @Override
    public Instant getCurrentTime(String url) {
        try {
            String questionId = extractQuestionId(url);

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

    private String extractQuestionId(String url) {
        return url.split("/questions/")[1].split("/")[0];
    }
}
