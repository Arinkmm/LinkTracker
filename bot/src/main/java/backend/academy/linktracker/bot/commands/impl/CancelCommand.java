package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import com.pengrad.telegrambot.model.Message;
import org.springframework.stereotype.Component;

@Component
public class CancelCommand extends AbstractCommand {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final MessagesProperties messagesProperties;

    public CancelCommand(
            CommandProperties commandProperties,
            UserService userService,
            TelegramSender telegramSender,
            MessagesProperties messagesProperties) {
        super(commandProperties, InternalCommand.CANCEL.configKey);
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.messagesProperties = messagesProperties;
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        userService.deleteState(id);
        userService.deleteUrl(id);
        telegramSender.sendMessage(id, messagesProperties.getCanceling());
    }
}
