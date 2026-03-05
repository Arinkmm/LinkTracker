package backend.academy.linktracker.scrapper.server.controller;

import backend.academy.linktracker.scrapper.client.bot.dto.*;  // ✅ client.bot!
import backend.academy.linktracker.scrapper.service.ScrapperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
public class LinkController {
    private final ScrapperService service;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        return ResponseEntity.ok(service.getLinks(chatId));
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
        @RequestHeader("Tg-Chat-Id") long chatId, @RequestBody AddLinkRequest request) {
        return ResponseEntity.ok(service.addLink(chatId, request));
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
        @RequestHeader("Tg-Chat-Id") long chatId, @RequestBody RemoveLinkRequest request) {
        return ResponseEntity.ok(service.removeLink(chatId, request));
    }
}
