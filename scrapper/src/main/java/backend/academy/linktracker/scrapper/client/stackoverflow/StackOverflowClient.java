package backend.academy.linktracker.scrapper.client.stackoverflow;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import java.time.Instant;
import java.util.List;

@HttpExchange
public interface StackOverflowClient {
    @GetExchange("/questions/{ids}")
    StackOverflowResponse getQuestions(@PathVariable String ids, @RequestParam String site, @RequestParam String filter);
}
