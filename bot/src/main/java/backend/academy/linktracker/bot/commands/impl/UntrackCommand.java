package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UntrackCommand implements Command {
    private final CommandProperties commandProperties;
    private final CommandExecutor commandExecutor;

    @Override
    public String command() {
        return commandProperties
                .getCommands()
                .get(InternalCommand.UNTRACK.configKey)
                .getName();
    }

    @Override
    public String description() {
        return commandProperties
                .getCommands()
                .get(InternalCommand.UNTRACK.configKey)
                .getDescription();
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        commandExecutor.executeUntrack(id);
    }
}
