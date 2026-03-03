package backend.academy.linktracker.bot.service.update;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateHandler {
    private final CommandRegistry commandRegistry;
    private final TelegramSender telegramSender;
    private final CommandProperties commandProperties;

    public void process(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return;
        }

        Message message = update.message();
        Long chatId = message.chat().id();
        Long userId = message.from().id();
        String text = message.text();

        log.atInfo()
                .addKeyValue("user_id", userId)
                .addKeyValue("chat_id", chatId)
                .addKeyValue("text", text)
                .log("Processing message");

        Command command = commandRegistry.find(text);
        if (command != null) {
            command.handle(message);
            return;
        }

        log.atWarn().addKeyValue("chat_id", chatId).addKeyValue("text", text).log("Unknown command received");
        telegramSender.sendMessage(chatId, commandProperties.getMessages().getUnknownCommand());
    }
}
