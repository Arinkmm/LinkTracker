package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class ChatController {

    private final UserService service;

    @PostMapping("/{id}")
    public ResponseEntity<Void> register(@PathVariable Long id) {
        service.registerChat(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteChat(id);
        return ResponseEntity.ok().build();
    }
}
