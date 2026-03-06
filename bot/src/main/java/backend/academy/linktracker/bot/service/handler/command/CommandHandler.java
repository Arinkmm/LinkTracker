package backend.academy.linktracker.bot.service.handler.command;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import backend.academy.linktracker.bot.service.user.UserService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandHandler {
    private final CommandRegistry registry;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public boolean tryHandle(Message message) {
        Command command = registry.find(message.text());
        if (command == null) return false;

        Long id = message.chat().id();
        userService.deleteState(id);
        userService.deleteUrl(id);

        try {
            command.handle(message);
        } catch (ApiException e) {
            telegramSender.sendMessage(id, e.getApiError().description());
        }
        return true;
    }
}

