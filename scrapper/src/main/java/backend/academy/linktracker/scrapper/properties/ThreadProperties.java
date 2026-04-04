package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.treading")
@Getter
@Setter
public class ThreadProperties {
    private int executedThreads;
}
