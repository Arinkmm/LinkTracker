package backend.academy.linktracker.scrapper.client.api.github;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import java.util.List;

@HttpExchange
public interface GitHubClient {
    @GetExchange("/repos/{owner}/{repo}/issues")
    List<GitHubRepoResponse> getIssues(@PathVariable String owner, @PathVariable String repo, @RequestParam String since);
}
