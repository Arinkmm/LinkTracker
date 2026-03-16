package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import java.net.URI;
import java.time.Instant;
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
        String path = url.getPath();

        return host != null
                && (host.equals("github.com") || host.equals("www.github.com"))
                && path != null
                && path.split("/").length >= 3;
    }

    @Override
    public Optional<Instant> getCurrentTime(URI url) {
        try {
            String[] parts = parseRepo(url);
            String owner = parts[0];
            String repo = parts[1];

            log.atDebug().addKeyValue("owner", owner).addKeyValue("repo", repo).log("Fetching GitHub repo time");

            return Optional.ofNullable(client.getRepository(owner, repo).updatedAt());
        } catch (Exception e) {
            log.atWarn()
                    .addKeyValue("url", url)
                    .addKeyValue("error", e.getClass().getSimpleName())
                    .log("Failed to get GitHub time");

            return Optional.empty();
        }
    }

    private String[] parseRepo(URI url) {
        String path = url.getPath();
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
