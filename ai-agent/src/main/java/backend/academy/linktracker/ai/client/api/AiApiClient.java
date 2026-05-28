package backend.academy.linktracker.ai.client.api;

import backend.academy.linktracker.ai.dto.ChatCompletionRequest;
import backend.academy.linktracker.ai.dto.ChatCompletionResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface AiApiClient {
    @PostExchange
    ChatCompletionResponse summarize(@RequestBody ChatCompletionRequest request);
}
