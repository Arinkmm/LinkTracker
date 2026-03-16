package backend.academy.linktracker.bot.util.url.validator;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlValidator {
    private final List<SpecificUrlValidator> specificUrlValidators;

    public boolean isValid(URI url) {
        if (url == null || url.getHost() == null) {
            return false;
        }
        return specificUrlValidators.stream().anyMatch(specificUrlValidator -> specificUrlValidator.isValid(url));
    }
}
