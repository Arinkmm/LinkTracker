package backend.academy.linktracker.bot.commands;

import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {
    private final TelegramSender telegramSender;
    private final CommandProperties commandProperties;

    @Setter
    private String commandList;

    @Override
    public String command() {
        return commandProperties.getCommands().getHelp().getName();
    }

    @Override
    public String description() {
        return commandProperties.getCommands().getHelp().getDescription();
    }

    @Override
    public void handle(Message message) {
        Long chatId = message.chat().id();
        telegramSender.sendMessage(chatId, commandProperties.getMessages().getHelpHeader() + "\n" + commandList);
    }
}
