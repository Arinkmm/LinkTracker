package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final UserService service;

    @PostMapping("/{id}")
    public ResponseEntity<Void> register(@PathVariable Long id) {
        log.atInfo()
            .addKeyValue("id", id)
            .log("Registering telegram chat");

        service.registerChat(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.atInfo()
            .addKeyValue("id", id)
            .log("Deleting telegram chat");

        service.deleteChat(id);
        return ResponseEntity.ok().build();
    }
}
