package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.LinkResponse;
import java.util.List;

public record GitHubRepoResponses(List<GitHubRepoResponse> issues) implements LinkResponse {}
