package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {
    private final CommandExecutor commandExecutor;
    private final CommandProperties commandProperties;

    @Override
    public String command() {
        return commandProperties.getCommands().get(InternalCommand.HELP.configKey).getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().get(InternalCommand.HELP.configKey).getDescription();
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        commandExecutor.executeHelp(id);
    }
}
