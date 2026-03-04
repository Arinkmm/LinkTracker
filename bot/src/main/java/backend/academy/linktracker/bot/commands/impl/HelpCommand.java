package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {
    private final TelegramSender telegramSender;
    private final CommandProperties commandProperties;

    @Override
    public String command() {
        return commandProperties.getCommands().get("help").getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().get("help").getDescription();
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        StringBuilder helpText =
                new StringBuilder(commandProperties.getMessages().getHelpHeader());

        commandProperties.getCommands().values().forEach(cmd -> helpText.append("\n- ")
                .append(cmd.getName())
                .append(" — ")
                .append(cmd.getDescription()));

        telegramSender.sendMessage(chatId, helpText.toString());
    }
}
