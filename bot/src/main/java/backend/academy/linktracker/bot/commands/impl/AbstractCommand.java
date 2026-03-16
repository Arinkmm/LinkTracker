package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.properties.CommandProperties;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractCommand implements Command {
    protected final CommandProperties commandProperties;
    private final String configKey;

    @Override
    public String command() {
        return commandProperties.getCommands().get(configKey).getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().get(configKey).getDescription();
    }
}
