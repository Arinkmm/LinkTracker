package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {
    public final Defaulting defaulting;
    public final Error error;
    public final String divider;
    public final String dateTimeFormat;
    public final Labels labels;
    public final Limits limits;

    @Getter
    @Setter
    public static class Labels {
        private String title;
        private String question;
        private String author;
        private String time;
        private String description;
        private String typeIssue;
        private String typePr;
        private String typeAnswer;
        private String typeComment;
        private String emptyContent;
    }

    @Getter
    @Setter
    public static class Limits {
        private int githubBody;
        private int stackoverflowAnswer;
        private int stackoverflowComment;
    }

    @Getter
    @Setter
    public static class Defaulting {
        private String title;
    }

    @Getter
    @Setter
    public static class Error {
        private String title;
    }
}
