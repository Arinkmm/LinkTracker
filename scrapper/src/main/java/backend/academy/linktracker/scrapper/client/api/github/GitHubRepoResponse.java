package backend.academy.linktracker.scrapper.client.api.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GitHubRepoResponse(
        @JsonProperty("title") String title,
        @JsonProperty("user") User user,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("body") String body,
        @JsonProperty("pull_request") Object pullRequest) {
    public boolean isPullRequest() {
        return pullRequest != null;
    }

    public record User(@JsonProperty("login") String login) {}
}
