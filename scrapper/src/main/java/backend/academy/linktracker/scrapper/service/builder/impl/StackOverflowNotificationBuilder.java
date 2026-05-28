package backend.academy.linktracker.scrapper.service.builder.impl;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import backend.academy.linktracker.scrapper.client.api.stackoverflow.StackOverflowResponse;
import backend.academy.linktracker.scrapper.properties.NotificationProperties;
import backend.academy.linktracker.scrapper.service.builder.NotificationBuilder;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackOverflowNotificationBuilder extends AbstractNotificationBuilder implements NotificationBuilder {
    private final NotificationProperties properties;

    @Override
    public boolean supports(LinkResponse response) {
        return response instanceof StackOverflowResponse;
    }

    @Override
    public String buildMessage(LinkResponse response) {
        StringBuilder sb = new StringBuilder();
        StackOverflowResponse stackOverflowResponse = (StackOverflowResponse) response;
        NotificationProperties.Labels labels = properties.getLabels();

        for (StackOverflowResponse.Item item : stackOverflowResponse.items()) {
            String question = cleanHtml(item.body());

            for (StackOverflowResponse.Answer answer : item.answers()) {
                String body = cleanHtml(answer.body());
                appendEntity(
                        sb,
                        labels.getTypeAnswer(),
                        question,
                        answer.owner().displayName(),
                        Instant.ofEpochSecond(answer.creationDate()),
                        body);
            }

            for (StackOverflowResponse.Comment comment : item.comments()) {
                String body = cleanHtml(comment.body());
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
                .append(formatDate(time, properties))
                .append("\n")
                .append(labels.getDescription())
                .append(body)
                .append("\n")
                .append(properties.getDivider())
                .append("\n");
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
