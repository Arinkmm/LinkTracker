package backend.academy.linktracker.scrapper.service.provider;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

public interface LinkTimeProvider {
    boolean supports(URI url);

    Optional<Instant> getCurrentTime(URI url);
}
