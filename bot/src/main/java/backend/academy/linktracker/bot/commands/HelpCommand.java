package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class HelpCommand implements Command {
    private final List<Command> commands;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Вывести список доступных команд";
    }

    @Override
    public SendMessage handle(Update update) {
        String text = commands.stream()
            .map(c -> c.command() + " - " + c.description())
            .collect(Collectors.joining("\n"));

        Long chatId = update.message().chat().id();
        return new SendMessage(chatId, "Доступные команды:\n" + text);
    }
}
