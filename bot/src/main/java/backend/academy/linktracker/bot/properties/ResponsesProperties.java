package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bot.responses")
public class ResponsesProperties {
    private String neededTags;
    private String notNeededTags;
}
