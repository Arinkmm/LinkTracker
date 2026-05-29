package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.api.UpdatesApi;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.metrics.BotMetrics;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.updates.type", havingValue = "http")
@RequiredArgsConstructor
@Slf4j
public class UpdateController implements UpdatesApi {
    private final TelegramSender telegramSender;
    private final BotMetrics metrics;

    @Override
    public ResponseEntity<Void> updatesPost(LinkUpdate update) {
        metrics.incrementTelegramRequest("http_update");
        log.atInfo()
                .addKeyValue("chat_count", update.getTgChatIds().size())
                .addKeyValue("description", update.getDescription())
                .log("Sending link update");

        update.getTgChatIds().forEach(id -> {
            int sentMessages = telegramSender.sendMessage(id, update.getDescription());
            metrics.incrementSentNotification(sentMessages);
        });

        return ResponseEntity.ok().build();
    }
}
