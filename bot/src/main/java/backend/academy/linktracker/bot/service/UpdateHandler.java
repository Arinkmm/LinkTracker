package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.commands.Command;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UpdateHandler {
    private final List<Command> commands;

    public SendMessage process(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return null;
        }

        Long chatId = update.message().chat().id();
        String text = update.message().text();
        String username = update.message().chat().username();

        log.info("Processing message: username = {}, chat_id = {}, text = {}", username, chatId, text);

        for (Command command : commands) {
            if (command.supports(update)) {
                return command.handle(update);
            }
        }
        log.warn("Unknown command received: chat_id = {}, text = {}", chatId, text);
        return new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд");
    }
}
