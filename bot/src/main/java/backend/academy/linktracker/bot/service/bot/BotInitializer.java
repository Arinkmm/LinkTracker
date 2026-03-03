package backend.academy.linktracker.bot.service.bot;

import backend.academy.linktracker.bot.service.command.CommandRegistry;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SetMyCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotInitializer implements CommandLineRunner {
    private final TelegramBot bot;
    private final TelegramBotListener telegramBotListener;
    private final CommandRegistry commandRegistry;

    @Override
    public void run(String... args) {
        setupMenu();
        bot.setUpdatesListener(telegramBotListener, e -> {
            if (e.response() != null) {
                log.atError()
                        .addKeyValue("error_code", e.response().errorCode())
                        .addKeyValue("description", e.response().description())
                        .log("Error from Telegram API");
            } else {
                log.atError().setCause(e).log("Network error from Telegram API");
            }
        });

        log.atInfo().log("Telegram bot started successfully");
    }

    private void setupMenu() {
        BotCommand[] botCommands = commandRegistry.getCommands().stream()
                .map(c -> new BotCommand(c.command(), c.description()))
                .toArray(BotCommand[]::new);
        bot.execute(new SetMyCommands(botCommands));

        log.atInfo().addKeyValue("commands_count", botCommands.length).log("Menu commands configured");
    }
}
