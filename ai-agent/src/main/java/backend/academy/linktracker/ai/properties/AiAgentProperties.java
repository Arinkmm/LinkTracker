package backend.academy.linktracker.ai.properties;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "ai-agent")
public class AiAgentProperties {
    private final Filtering filtering;
    private final Summarization summarization;
    private final Labels labels;

    @Getter
    @RequiredArgsConstructor
    public static class Filtering {
        private final List<String> stopWords;
        private final List<String> excludedAuthors;
        private final int minLength;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Summarization {
        private final int threshold;
        private final Api api;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Api {
        private final String url;
        private final String token;
        private final String model;
        private final Duration timeout;
        private final String prompt;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Labels {
        private final String description;
    }
}
