package backend.academy.linktracker.bot.commands;

import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
    private final TelegramSender telegramSender;
    private final CommandProperties commandProperties;

    @Override
    public String command() {
        return commandProperties.getCommands().getStart().getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().getStart().getDescription();
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        telegramSender.sendMessage(chatId, commandProperties.getMessages().getWelcome());
    }
}
