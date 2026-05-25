package backend.academy.linktracker.ai.summarization;

import backend.academy.linktracker.ai.client.api.AiApiClient;
import backend.academy.linktracker.ai.dto.ChatCompletionRequest;
import backend.academy.linktracker.ai.dto.ChatCompletionResponse;
import backend.academy.linktracker.ai.dto.ChatMessage;
import backend.academy.linktracker.ai.exception.AiSummarizationException;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiApiUpdateSummarizer {
    private final AiApiClient aiApiClient;
    private final AiAgentProperties properties;

    public String summarize(String text) {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        ChatCompletionRequest request = new ChatCompletionRequest(
                api.getModel(), List.of(new ChatMessage("user", api.getPrompt() + "\n\n" + text)), false);

        try {
            ChatCompletionResponse response = aiApiClient.summarize(request);
            return extractSummary(response);
        } catch (RestClientException e) {
            log.atError().setCause(e).log("AI API summarization request failed");
            throw new AiSummarizationException("AI API summarization request failed", e);
        }
    }

    private String extractSummary(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiSummarizationException("AI API response does not contain choices");
        }

        ChatMessage message = response.choices().getFirst().message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new AiSummarizationException("AI API response does not contain summary text");
        }

        return message.content().trim();
    }
}
