package backend.academy.linktracker.scrapper.service.builder;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponse;
import backend.academy.linktracker.scrapper.client.api.github.GitHubRepoResponses;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowResponse;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationBuilder {
    private final NotificationProperties properties;

    public String buildError(Link link) {
        return properties.getError().getTitle() + "\n\n" + link.url() + "\n";
    }

    public String buildMessage(LinkResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append(properties.getDefaulting().getTitle()).append("\n\n");

        if (response instanceof GitHubRepoResponses r) {
            sb.append(buildGitHubMessage(r));
        } else if (response instanceof StackOverflowResponse r) {
            sb.append(buildStackOverflowMessage(r));
        }
        return sb.toString();
    }

    private String buildGitHubMessage(GitHubRepoResponses response) {
        StringBuilder sb = new StringBuilder();
        NotificationProperties.Labels labels = properties.getLabels();
        NotificationProperties.Limits limits = properties.getLimits();

        for (GitHubRepoResponse r : response.issues()) {
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
                    .append(formatDate(r.createdAt()))
                    .append("\n")
                    .append(labels.getDescription())
                    .append(body)
                    .append("\n")
                    .append(properties.getDivider())
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildStackOverflowMessage(StackOverflowResponse response) {
        StringBuilder sb = new StringBuilder();
        NotificationProperties.Labels labels = properties.getLabels();
        NotificationProperties.Limits limits = properties.getLimits();

        for (StackOverflowResponse.Item item : response.items()) {
            String question = cleanHtml(item.body());

            for (StackOverflowResponse.Answer answer : item.answers()) {
                String body = truncate(cleanHtml(answer.body()), limits.getStackoverflowAnswer());
                appendEntity(
                        sb,
                        labels.getTypeAnswer(),
                        question,
                        answer.owner().displayName(),
                        Instant.ofEpochSecond(answer.creationDate()),
                        body);
            }

            for (StackOverflowResponse.Comment comment : item.comments()) {
                String body = truncate(cleanHtml(comment.body()), limits.getStackoverflowComment());
                appendEntity(
                        sb,
                        labels.getTypeComment(),
                        question,
                        comment.owner().displayName(),
                        Instant.ofEpochSecond(comment.creationDate()),
                        body);
            }
        }
        return sb.toString();
    }

    private void appendEntity(
            StringBuilder sb, String type, String question, String author, Instant time, String body) {
        NotificationProperties.Labels labels = properties.getLabels();
        sb.append(type)
                .append("\n")
                .append(labels.getQuestion())
                .append(question)
                .append("\n")
                .append(labels.getAuthor())
                .append(author)
                .append("\n")
                .append(labels.getTime())
                .append(formatDate(time))
                .append("\n")
                .append(labels.getDescription())
                .append(body)
                .append("\n")
                .append(properties.getDivider())
                .append("\n");
    }

    private String formatDate(Instant instant) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(properties.getDateTimeFormat()).withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }

    private String truncate(String text, int limit) {
        if (text.length() <= limit) return text;
        return text.substring(0, limit) + "...";
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

    private String cleanHtml(String text) {
        if (text == null) return properties.getLabels().getEmptyContent();

        Document doc = Jsoup.parse(text);

        doc.select("pre").forEach(el -> el.replaceWith(new TextNode(" [код] ")));
        doc.select("code").forEach(el -> el.replaceWith(new TextNode(" [код] ")));
        doc.select("img").forEach(el -> el.replaceWith(new TextNode(" [изображение] ")));

        return doc.text().trim();
    }
}
