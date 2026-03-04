package backend.academy.linktracker.bot.properties;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bot")
public class CommandProperties {
    private Messages messages;
    private Map<String, CommandInfo> commands = new HashMap<>();

    @Getter
    @Setter
    public static class Messages {
        private String welcome;
        private String helpHeader;
        private String unknownCommand;
    }

    @Getter
    @Setter
    public static class CommandInfo {
        private String name;
        private String description;
    }
}
