package backend.academy.linktracker.bot.util;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {
    public Message validate(Update update) {
        return update.message() != null && update.message().text() != null ? update.message() : null;
    }
}
