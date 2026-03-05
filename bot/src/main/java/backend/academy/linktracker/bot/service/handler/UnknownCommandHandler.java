package backend.academy.linktracker.bot.service.handler;

import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnknownCommandHandler {
    private final TelegramSender telegramSender;
    private final CommandProperties properties;

    public void handle(Long chatId) {
        telegramSender.sendMessage(chatId, properties.getMessages().getUnknownCommand());
    }
}
