package backend.academy.linktracker.bot.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bot.commands")
public class CommandProperties {
    private Map<String, CommandInfo> commands = new HashMap<>();

    @Getter
    @Setter
    public static class CommandInfo {
        private String name;
        private String description;
    }
}
