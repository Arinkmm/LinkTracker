package backend.academy.linktracker.scrapper.service.provider.impl;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.service.provider.LinkTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
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
            return client.getRepository(parts[0], parts[1]).updatedAt();
        } catch (Exception e) {
            return Instant.EPOCH;
        }
    }

    private String[] parseRepo(String url) {
        return url.split("github\\.com/")[1].split("/", 2);
    }
}

