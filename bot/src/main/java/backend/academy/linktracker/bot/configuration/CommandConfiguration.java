package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.HelpCommand;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandConfiguration {
    @Bean
    public CommandRegistry commandRegistry(List<Command> commands, HelpCommand helpCommand) {
        CommandRegistry registry = new CommandRegistry(commands);
        helpCommand.setCommandList(registry.buildCommandList());
        return registry;
    }
}
