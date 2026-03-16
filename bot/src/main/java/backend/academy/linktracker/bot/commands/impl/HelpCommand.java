package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand extends AbstractCommand {
    private final TelegramSender telegramSender;
    private final MessagesProperties messagesProperties;

    public HelpCommand(
            CommandProperties commandProperties, TelegramSender telegramSender, MessagesProperties messagesProperties) {
        super(commandProperties, InternalCommand.HELP.configKey);
        this.telegramSender = telegramSender;
        this.messagesProperties = messagesProperties;
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        StringBuilder helpText = new StringBuilder(messagesProperties.getHelpHeader());

        commandProperties.getCommands().values().forEach(cmd -> helpText.append("\n- ")
                .append(cmd.getName())
                .append(" — ")
                .append(cmd.getDescription()));

        telegramSender.sendMessage(id, helpText.toString());
    }
}
