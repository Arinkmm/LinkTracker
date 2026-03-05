package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
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
            return client.getQuestion(questionId).lastActivity();
        } catch (Exception e) {
            return Instant.EPOCH;
        }
    }

    private String extractQuestionId(String url) {
        return url.split("/questions/")[1].split("/")[0];
    }
}

