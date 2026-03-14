package backend.academy.linktracker.scrapper.service.provider;

import java.net.URI;
import java.time.Instant;

public interface LinkTimeProvider {
    boolean supports(URI url);

    Instant getCurrentTime(URI url);
}
