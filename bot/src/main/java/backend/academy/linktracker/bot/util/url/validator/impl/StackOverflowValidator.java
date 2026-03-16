package backend.academy.linktracker.bot.util.url.validator.impl;

import backend.academy.linktracker.bot.util.url.validator.SpecificUrlValidator;
import java.net.URI;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StackOverflowValidator implements SpecificUrlValidator {
    private static final Pattern SO_PATH = Pattern.compile("^/questions/\\d+(/.*)?$");

    @Override
    public boolean isValid(URI url) {
        String host = url.getHost();
        return host.contains("stackoverflow.com")
                && SO_PATH.matcher(url.getPath()).matches();
    }
}
