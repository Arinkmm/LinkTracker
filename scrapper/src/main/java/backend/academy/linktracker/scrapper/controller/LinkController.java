package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.api.LinksApi;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LinkController implements LinksApi {
    private final LinkService service;
    private final ScrapperMetrics metrics;

    @Override
    public ResponseEntity<ListLinksResponse> linksGet(Long tgChatId) {
        metrics.incrementApiRequest("links_get");
        log.atDebug().addKeyValue("id", tgChatId).log("Getting links");

        return ResponseEntity.ok(service.getLinks(tgChatId));
    }

    @Override
    public ResponseEntity<LinkResponse> linksPost(Long tgChatId, AddLinkRequest addLinkRequest) {
        metrics.incrementApiRequest("links_post");
        log.atInfo()
                .addKeyValue("id", tgChatId)
                .addKeyValue("url", addLinkRequest.getLink())
                .addKeyValue(
                        "tags_count",
                        (addLinkRequest.getTags() != null)
                                ? addLinkRequest.getTags().size()
                                : 0)
                .log("Adding link");

        return ResponseEntity.ok(service.addLink(tgChatId, addLinkRequest));
    }

    @Override
    public ResponseEntity<LinkResponse> linksDelete(Long tgChatId, RemoveLinkRequest removeLinkRequest) {
        metrics.incrementApiRequest("links_delete");
        log.atInfo()
                .addKeyValue("id", tgChatId)
                .addKeyValue("url", removeLinkRequest.getLink())
                .log("Removing link");

        return ResponseEntity.ok(service.removeLink(tgChatId, removeLinkRequest));
    }
}
