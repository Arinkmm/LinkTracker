package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.threading")
@Getter
@Setter
public class ThreadProperties {
    @Min(1)
    private int poolSize;
}
