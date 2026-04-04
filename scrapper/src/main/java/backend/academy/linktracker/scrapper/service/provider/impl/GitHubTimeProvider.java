package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.LinkResponse;
import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.github.GitHubRepoResponses;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubTimeProvider implements LinkTimeProvider {
    private final GitHubClient client;

    @Override
    public boolean supports(URI url) {
        String host = url.getHost();
        boolean supports = host != null
            && (host.equals("github.com") || host.equals("www.github.com"))
            && url.getPath() != null;

        if (supports) {
            log.atTrace().addKeyValue("url", url).log("GitHub provider supports this URL");
        }
        return supports;
    }

    @Override
    public LinkResponse getResponse(Link link) {
        String owner = "unknown";
        String repo = "unknown";
        try {
            String[] parts = parseRepo(link.url());
            owner = parts[0];
            repo = parts[1];

            Instant since = Optional.ofNullable(link.lastChecked()).orElse(Instant.EPOCH);

            log.atDebug()
                .addKeyValue("owner", owner)
                .addKeyValue("repo", repo)
                .addKeyValue("since", since)
                .log("Fetching GitHub updates");

            List<GitHubRepoResponse> issues = client.getIssues(owner, repo, since.toString())
                .stream()
                .filter(r -> r.createdAt().isAfter(since))
                .toList();

            log.atInfo()
                .addKeyValue("repo", owner + "/" + repo)
                .addKeyValue("newEventsCount", issues.size())
                .log("GitHub sync successful");

            return new GitHubRepoResponses(issues);
        } catch (IllegalArgumentException e) {
            log.atWarn()
                .addKeyValue("url", link.url())
                .addKeyValue("error", e.getMessage())
                .log("Failed to parse GitHub repository from URL");
            return null;
        } catch (Exception e) {
            log.atError()
                .setCause(e)
                .addKeyValue("owner", owner)
                .addKeyValue("repo", repo)
                .addKeyValue("url", link.url())
                .log("Unexpected error while fetching GitHub response");
            return null;
        }
    }

    private String[] parseRepo(URI url) {
        String path = url.getPath();
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path is empty");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] segments = path.split("/");
        if (segments.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL path: " + path);
        }

        return new String[] {segments[0], segments[1]};
    }
}
