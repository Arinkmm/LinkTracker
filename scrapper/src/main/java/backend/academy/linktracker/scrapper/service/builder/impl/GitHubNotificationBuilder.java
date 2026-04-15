package backend.academy.linktracker.scrapper.service.builder.impl;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponses;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import backend.academy.linktracker.scrapper.service.builder.NotificationBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GitHubNotificationBuilder extends AbstractNotificationBuilder implements NotificationBuilder {
    private final NotificationProperties properties;

    @Override
    public boolean supports(LinkResponse response) {
        return response instanceof GitHubRepoResponses;
    }

    @Override
    public String buildMessage(LinkResponse response) {
        StringBuilder sb = new StringBuilder();
        GitHubRepoResponses gitHubResponse = (GitHubRepoResponses) response;
        NotificationProperties.Labels labels = properties.getLabels();
        NotificationProperties.Limits limits = properties.getLimits();

        for (GitHubRepoResponse r : gitHubResponse.issues()) {
            String type = r.isPullRequest() ? labels.getTypePr() : labels.getTypeIssue();
            String body = truncate(cleanMarkdown(r.body()), limits.getGithubBody());

            sb.append(type)
                    .append("\n")
                    .append(labels.getTitle())
                    .append(r.title())
                    .append("\n")
                    .append(labels.getAuthor())
                    .append(r.user().login())
                    .append("\n")
                    .append(labels.getTime())
                    .append(formatDate(r.createdAt(), properties))
                    .append("\n")
                    .append(labels.getDescription())
                    .append(body)
                    .append("\n")
                    .append(properties.getDivider())
                    .append("\n");
        }
        return sb.toString();
    }

    private String cleanMarkdown(String text) {
        if (text == null) return properties.getLabels().getEmptyContent();

        String noMarkdown = text.replaceAll("```[\\s\\S]*?```", "[код]")
                .replaceAll("`[^`]+`", "[код]")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "[изображение]")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("[#*_~>]", "");

        return Jsoup.parse(noMarkdown).text().trim();
    }
}
