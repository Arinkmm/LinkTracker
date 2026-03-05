package backend.academy.linktracker.scrapper.service.notifier;

import java.time.Instant;

public interface LinkTimeProvider {
    boolean supports(String url);
    Instant getCurrentTime(String url);
}

