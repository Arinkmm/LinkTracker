package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.api.UpdatesApi;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UpdateController implements UpdatesApi {
    private final TelegramSender telegramSender;

    @Override
    public ResponseEntity<Void> updatesPost(LinkUpdate update) {
        log.atInfo()
                .addKeyValue("chat_count", update.getTgChatIds().size())
                .addKeyValue("description", update.getDescription())
                .log("Sending link update");

        update.getTgChatIds().forEach(id -> telegramSender.sendMessage(id, update.getDescription()));

        return ResponseEntity.ok().build();
    }
}
