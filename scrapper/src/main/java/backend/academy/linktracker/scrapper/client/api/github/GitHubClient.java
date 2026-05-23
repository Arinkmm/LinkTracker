package backend.academy.linktracker.scrapper.client.api.github;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface GitHubClient {
    @GetExchange("/repos/{owner}/{repo}/issues")
    @Retry(name = "github-api")
    @CircuitBreaker(name = "github-api")
    List<GitHubRepoResponse> getIssues(
            @PathVariable String owner, @PathVariable String repo, @RequestParam String since);
}
