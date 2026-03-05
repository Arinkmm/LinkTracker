package backend.academy.linktracker.bot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlValidator {
    private static final Pattern GITHUB_REPO =
            Pattern.compile("https?://(?:www\\.)?github\\.com/([a-zA-Z0-9\\-_.]+)/([a-zA-Z0-9\\-_./]+?)(?:/|$)");
    private static final Pattern SO_QUESTION =
            Pattern.compile("https?://(?:[^/]+\\.)?stackoverflow\\.com/questions?/(\\d+)(?:/[^/]+)?");

    public boolean isValid(String url) {
        return isGitHubRepo(url) || isSOQuestion(url);
    }

    private boolean isGitHubRepo(String url) {
        Matcher matcher = GITHUB_REPO.matcher(url);
        return matcher.matches();
    }

    private boolean isSOQuestion(String url) {
        Matcher matcher = SO_QUESTION.matcher(url);
        return matcher.matches();
    }
}
