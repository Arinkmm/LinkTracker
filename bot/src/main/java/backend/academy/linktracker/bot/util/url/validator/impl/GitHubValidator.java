package backend.academy.linktracker.bot.util.url.validator.impl;

import backend.academy.linktracker.bot.util.url.validator.SpecificUrlValidator;
import java.net.URI;
import java.util.regex.Pattern;

public class GitHubValidator implements SpecificUrlValidator {
    private static final Pattern GITHUB_PATH = Pattern.compile("^/[a-zA-Z0-9\\-_.]+/[a-zA-Z0-9\\-_.]+(/.*)?$");

    @Override
    public boolean isValid(URI url) {
        String host = url.getHost();
        return (host.equals("github.com") || host.equals("www.github.com"))
                && GITHUB_PATH.matcher(url.getPath()).matches();
    }
}
