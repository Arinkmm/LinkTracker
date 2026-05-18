package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@RequiredArgsConstructor
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    @NotNull
    private Duration l1Ttl;

    @NotNull
    private Duration l2Ttl;

    @Positive
    private int l1Capacity;
}
