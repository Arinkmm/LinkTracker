package backend.academy.linktracker.bot.service.command;

import backend.academy.linktracker.bot.commands.Command;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CommandRegistry {
    private final Map<String, Command> commandMap;

    public CommandRegistry(List<Command> commands) {
        commandMap = commands.stream().collect(Collectors.toMap(Command::command, Function.identity()));
    }

    public Command find(String commandText) {
        if (commandText == null || !commandText.startsWith("/")) {
            return null;
        }

        String commandName =
                commandText.contains(" ") ? commandText.substring(0, commandText.indexOf(' ')) : commandText;

        return commandMap.get(commandName);
    }

    public List<Command> getCommands() {
        return List.copyOf(commandMap.values());
    }

    public String buildCommandList() {
        return commandMap.values().stream()
                .map(c -> c.command() + " - " + c.description())
                .collect(Collectors.joining("\n"));
    }
}
