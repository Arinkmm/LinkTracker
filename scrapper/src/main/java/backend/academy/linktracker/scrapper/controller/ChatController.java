package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.TgChatApi;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.service.user.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController implements TgChatApi {
    private final ChatService service;
    private final ScrapperMetrics metrics;

    @Override
    public ResponseEntity<Void> tgChatIdPost(Long id) {
        metrics.incrementApiRequest(ScrapperMetrics.API_TG_CHAT_POST);
        log.atInfo().addKeyValue("id", id).log("Registering telegram chat");

        service.registerChat(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> tgChatIdDelete(Long id) {
        metrics.incrementApiRequest(ScrapperMetrics.API_TG_CHAT_DELETE);
        log.atInfo().addKeyValue("id", id).log("Deleting telegram chat");

        service.deleteChat(id);
        return ResponseEntity.ok().build();
    }
}
