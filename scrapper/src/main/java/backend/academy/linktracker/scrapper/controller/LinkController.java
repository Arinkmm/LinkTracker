package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/links")
@RequiredArgsConstructor
@Slf4j
public class LinkController {
    private final UserService service;

    @GetMapping
    public ResponseEntity<ListLinksResponse> getLinks(@RequestHeader("Tg-Chat-Id") long chatId) {
        log.atDebug()
            .addKeyValue("id", chatId)
            .log("Getting links");

        return ResponseEntity.ok(service.getLinks(chatId));
    }

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
        @RequestHeader("Tg-Chat-Id") Long id, @RequestBody AddLinkRequest request) {
        log.atInfo()
            .addKeyValue("id", id)
            .addKeyValue("url", request.link())
            .addKeyValue("tags_count", request.tags().size())
            .log("Adding link");

        return ResponseEntity.ok(service.addLink(id, request));
    }

    @DeleteMapping
    public ResponseEntity<LinkResponse> removeLink(
        @RequestHeader("Tg-Chat-Id") Long id, @RequestBody RemoveLinkRequest request) {
        log.atInfo()
            .addKeyValue("id", id)
            .addKeyValue("url", request.link())
            .log("Removing link");

        return ResponseEntity.ok(service.removeLink(id, request));
    }
}
