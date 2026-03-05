package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CancelCommand implements Command {
    private final CommandProperties commandProperties;
    private final UserService userService;
    private final TelegramSender telegramSender;

    @Override
    public String command() {
        return commandProperties.getCommands().get("cancel").getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().get("cancel").getDescription();
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        userService.deleteState(chatId);
        userService.deleteUrl(chatId);
        telegramSender.sendMessage(chatId, commandProperties.getMessages().getCanceling());
    }
}
