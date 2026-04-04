package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.LinkResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record GitHubRepoResponse(@JsonProperty("title") String title, @JsonProperty("user") User user, @JsonProperty("created_at") Instant createdAt, @JsonProperty("body") String body, @JsonProperty("pull_request") Object pullRequest) {
    public boolean isPullRequest() {
        return pullRequest != null;
    }
    public record User(@JsonProperty("login") String login) {}
}
