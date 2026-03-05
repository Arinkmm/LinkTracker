package backend.academy.linktracker.scrapper.client.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GitHubRepoResponse(@JsonProperty("updated_at") Instant updatedAt) {}
