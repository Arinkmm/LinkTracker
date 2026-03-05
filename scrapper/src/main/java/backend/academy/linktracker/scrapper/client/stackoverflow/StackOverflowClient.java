package backend.academy.linktracker.scrapper.client.stackoverflow;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface StackOverflowClient {
    @GetExchange("/questions/{id}")
    StackOverflowResponse getQuestion(@PathVariable String id, @RequestParam("site") String site);

    default StackOverflowResponse getQuestion(String id) {
        return getQuestion(id, "stackoverflow");
    }
}
