package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

    private final Defaulting defaulting;
    private final Error error;
    private final String divider;
    private final String dateTimeFormat;
    private final Labels labels;
    private final Limits limits;

    @Getter
    @RequiredArgsConstructor
    public static class Labels {
        private final String title;
        private final String question;
        private final String author;
        private final String time;
        private final String description;
        private final String typeIssue;
        private final String typePr;
        private final String typeAnswer;
        private final String typeComment;
        private final String emptyContent;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Limits {
        private final int githubBody;
        private final int stackoverflowAnswer;
        private final int stackoverflowComment;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Defaulting {
        private final String title;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Error {
        private final String title;
    }
}
