package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    private int l1Capacity;
}
