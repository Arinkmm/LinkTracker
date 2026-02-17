package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.commands.Command;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UpdateHandler {
    private final List<Command> commands;

    public SendMessage process(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return null;
        }

        Long chatId = update.message().chat().id();

        for (Command command : commands) {
            if (command.supports(update)) {
                return command.handle(update);
            }
        }
        return new SendMessage(chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд");
    }
}
