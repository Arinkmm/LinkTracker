package backend.academy.linktracker.bot.commands;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements Command {
    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Начать работу";
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.message().chat().id();
        return new SendMessage(chatId,"Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды");
    }
}
