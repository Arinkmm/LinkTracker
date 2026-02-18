package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.commands.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class TelegramBotListener implements UpdatesListener {
    private final TelegramBot bot;
    private final UpdateHandler updateHandler;
    private final List<Command> commands;

    @PostConstruct
    public void start() {
        setupMenu();
        bot.setUpdatesListener(this, e -> {
            if (e.response() != null) {
                log.error(
                        "Error from Telegram API: error_code = {}, description = {}",
                        e.response().errorCode(),
                        e.response().description());
            } else {
                log.error("Network error from Telegram API: {}", e.getMessage());
            }
        });

        log.info("Telegram bot started successfully");
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            try {
                SendMessage response = updateHandler.process(update);
                if (response != null) {
                    bot.execute(response);
                }
            } catch (Exception e) {
                log.error("Error processing update id = {}: {}", update.updateId(), e.getMessage());
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void setupMenu() {
        BotCommand[] botCommands = commands.stream()
                .map(c -> new BotCommand(c.command(), c.description()))
                .toArray(BotCommand[]::new);
        bot.execute(new SetMyCommands(botCommands));
        log.info("Menu commands configured. Count: {}", botCommands.length);
    }
}
