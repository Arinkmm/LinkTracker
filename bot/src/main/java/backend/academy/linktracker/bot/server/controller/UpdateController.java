package backend.academy.linktracker.bot.server.controller;

import backend.academy.linktracker.bot.client.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
@Slf4j
public class UpdateController {
    private final TelegramSender telegramSender;

    @PostMapping
    public ResponseEntity<Void> sendUpdate(@RequestBody LinkUpdate update) {
            update.tgChatIds().forEach(chatId ->
                telegramSender.sendMessage(chatId, update.description()));
            return ResponseEntity.ok().build();
    }
}
