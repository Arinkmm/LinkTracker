package backend.academy.linktracker.bot.service.handler.command;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import backend.academy.linktracker.bot.service.user.UserService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandHandler {
    private final CommandRegistry registry;
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final BotMetrics metrics;

    public boolean tryHandle(Message message) {
        Command command = registry.find(message.text());
        if (command == null) return false;

        Long id = message.chat().id();
        String commandText = command.command();

        log.atDebug().addKeyValue("id", id).addKeyValue("command", commandText).log("Executing command");

        metrics.incrementCommandRequest(commandText);
        metrics.recordCommandDuration(commandText, BotMetrics.SCOPE_SCRAPPER_SYNC_API, commandText, () -> {
            userService.deleteState(id);
            userService.deleteUrl(id);

            try {
                command.handle(message);
            } catch (ApiException e) {
                log.atError()
                        .addKeyValue("id", id)
                        .addKeyValue("command", commandText)
                        .addKeyValue("error_code", e.getApiError().getCode())
                        .addKeyValue("description", e.getApiError().getDescription())
                        .log("Command failed");

                telegramSender.sendMessage(id, e.getApiError().getDescription());
            }
        });
        return true;
    }
}
