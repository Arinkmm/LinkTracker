package backend.academy.linktracker.bot.util.url.validator;

import java.net.URI;

public interface SpecificUrlValidator {
    boolean isValid(URI url);
}
