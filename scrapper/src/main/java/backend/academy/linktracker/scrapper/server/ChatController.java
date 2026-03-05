package backend.academy.linktracker.scrapper.server;

import backend.academy.linktracker.scrapper.service.ScrapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class ChatController {

    private final ScrapperService service;

    @PostMapping("/{id}")
    public ResponseEntity<Void> register(@PathVariable long id) {
        service.registerChat(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.deleteChat(id);
        return ResponseEntity.ok().build();
    }
}
