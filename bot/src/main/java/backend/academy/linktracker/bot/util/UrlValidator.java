package backend.academy.linktracker.bot.util;

import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlValidator {
    private static final Pattern GITHUB_PATH = Pattern.compile("^/[a-zA-Z0-9\\-_.]+/[a-zA-Z0-9\\-_.]+(/.*)?$");
    private static final Pattern SO_PATH = Pattern.compile("^/questions/\\d+(/.*)?$");

    public boolean isValid(URI url) {
        if (url == null || url.getHost() == null) {
            return false;
        }
        return isGitHubRepo(url) || isSOQuestion(url);
    }

    private boolean isGitHubRepo(URI url) {
        String host = url.getHost();
        return (host.equals("github.com") || host.equals("www.github.com"))
                && GITHUB_PATH.matcher(url.getPath()).matches();
    }

    private boolean isSOQuestion(URI url) {
        String host = url.getHost();
        return host.contains("stackoverflow.com")
                && SO_PATH.matcher(url.getPath()).matches();
    }
}
