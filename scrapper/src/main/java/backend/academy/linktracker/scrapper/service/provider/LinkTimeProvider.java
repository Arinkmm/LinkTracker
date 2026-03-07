package backend.academy.linktracker.scrapper.service.provider;

import java.time.Instant;

public interface LinkTimeProvider {
    boolean supports(String url);

    Instant getCurrentTime(String url);
}
