package backend.academy.linktracker.bot.service.handler;

import backend.academy.linktracker.bot.service.handler.command.CommandHandler;
import backend.academy.linktracker.bot.service.handler.command.UnknownCommandHandler;
import backend.academy.linktracker.bot.service.handler.state.StateHandler;
import backend.academy.linktracker.bot.util.MessageValidator;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateHandler {
    private final MessageValidator validator;
    private final CommandHandler commandHandler;
    private final StateHandler stateHandler;
    private final UnknownCommandHandler unknownHandler;

    public void process(Update update) {
        Message message = validator.validate(update);
        if (message == null) return;

        Long id = message.chat().id();
        String text = message.text();

        log.atInfo().addKeyValue("id", id).addKeyValue("text", text).log("Processing message");

        if (commandHandler.tryHandle(message)) return;
        if (stateHandler.hasState(id)) {
            stateHandler.handle(id, text);
            return;
        }

        unknownHandler.handle(id);
    }
}
