package backend.academy.linktracker.scrapper.client.api.stackoverflow;

import backend.academy.linktracker.scrapper.client.api.LinkResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowResponse(@JsonProperty("items") List<Item> items) implements LinkResponse {
    public record Item(
            @JsonProperty("question_id") long questionId,
            @JsonProperty("body") String body,
            @JsonProperty("answers") List<Answer> answers,
            @JsonProperty("comments") List<Comment> comments,
            @JsonProperty("last_activity_date") long lastActivityDate) {}

    public record Answer(
            @JsonProperty("owner") Owner owner,
            @JsonProperty("body") String body,
            @JsonProperty("creation_date") long creationDate) {}

    public record Owner(@JsonProperty("display_name") String displayName) {}

    public record Comment(
            @JsonProperty("owner") Owner owner,
            @JsonProperty("body") String body,
            @JsonProperty("creation_date") long creationDate) {}
}
