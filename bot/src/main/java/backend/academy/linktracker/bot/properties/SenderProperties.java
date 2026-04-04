package backend.academy.linktracker.bot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "sender")
public class SenderProperties {
    private int messageLimit;
    private String continuer;
    private String delimiter;
}
