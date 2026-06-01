package backend.academy.linktracker.bot.service.bot;

import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.handler.UpdateHandler;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotListener implements UpdatesListener {
    private final UpdateHandler updateHandler;
    private final BotMetrics metrics;

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            metrics.incrementTelegramRequest(BotMetrics.REQUEST_TYPE_TELEGRAM_UPDATE);
            try {
                updateHandler.process(update);
            } catch (Exception e) {
                log.atError()
                        .setCause(e)
                        .addKeyValue("update_id", update.updateId())
                        .log("Error processing update");
            }
        }
        return CONFIRMED_UPDATES_ALL;
    }
}
