package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubTimeProvider implements LinkTimeProvider {
    private final GitHubClient client;

    @Override
    public boolean supports(String url) {
        return url.contains("github.com") && url.matches(".*github\\.com/[^/]+/[^/]+.*");
    }

    @Override
    public Instant getCurrentTime(String url) {
        try {
            String[] parts = parseRepo(url);
            String owner = parts[0];
            String repo = parts[1];

            log.atDebug()
                .addKeyValue("owner", owner)
                .addKeyValue("repo", repo)
                .log("Fetching GitHub repo time");

            return client.getRepository(owner, repo).updatedAt();
        } catch (Exception e) {
            log.atWarn()
                .addKeyValue("url", url)
                .addKeyValue("error", e.getClass().getSimpleName())
                .log("Failed to get GitHub time");

            return Instant.EPOCH;
        }
    }

    private String[] parseRepo(String url) {
        return url.split("github\\.com/")[1].split("/", 2);
    }
}
