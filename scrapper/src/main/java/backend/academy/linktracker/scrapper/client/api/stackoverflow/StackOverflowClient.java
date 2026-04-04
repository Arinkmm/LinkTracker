package backend.academy.linktracker.scrapper.client.api.stackoverflow;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface StackOverflowClient {
    @GetExchange("/questions/{ids}")
    StackOverflowResponse getQuestions(
            @PathVariable String ids, @RequestParam String site, @RequestParam String filter);
}
