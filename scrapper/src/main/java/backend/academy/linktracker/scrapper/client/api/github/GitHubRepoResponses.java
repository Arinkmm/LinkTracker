package backend.academy.linktracker.scrapper.client.api.github;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import java.util.List;

public record GitHubRepoResponses(List<GitHubRepoResponse> issues) implements LinkResponse {}
