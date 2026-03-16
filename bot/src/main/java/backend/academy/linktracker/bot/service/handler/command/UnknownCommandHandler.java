package backend.academy.linktracker.bot.service.handler.command;

import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnknownCommandHandler {
    private final TelegramSender telegramSender;
    private final MessagesProperties properties;

    public void handle(Long id) {
        telegramSender.sendMessage(id, properties.getUnknownCommand());
    }
}
