package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
    private final CommandExecutor commandExecutor;
    private final CommandProperties commandProperties;

    @Override
    public String command() {
        return commandProperties
                .getCommands()
                .get(InternalCommand.START.configKey)
                .getName();
    }

    @Override
    public String description() {
        return commandProperties
                .getCommands()
                .get(InternalCommand.START.configKey)
                .getDescription();
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        commandExecutor.executeStart(id);
    }
}
