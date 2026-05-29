package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.*;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.repository.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DBProperties dbProperties;
    private final ScrapperMetrics metrics;

    @Transactional
    @CacheEvict(value = "links", key = "#chatId")
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (!metrics.recordRequestDuration("database", "chats", () -> chatRepository.exists(chatId))) {
            throw new ChatNotFoundException();
        }

        Link link = metrics.recordRequestDuration("database", "links", () -> linkRepository.save(request.getLink()));

        if (metrics.recordRequestDuration(
                "database", "subscriptions", () -> subscriptionRepository.exists(link.id(), chatId))) {
            throw new LinkAlreadyTrackedException();
        }

        metrics.recordRequestDuration(
                "database", "subscriptions", () -> subscriptionRepository.save(chatId, link.id(), request.getTags()));
        return mapToResponse(link, request.getTags());
    }

    @Transactional
    @CacheEvict(value = "links", key = "#chatId")
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        if (!metrics.recordRequestDuration("database", "chats", () -> chatRepository.exists(chatId))) {
            throw new ChatNotFoundException();
        }

        Link link = metrics.recordRequestDuration(
                        "database", "links", () -> linkRepository.findByUrl(request.getLink()))
                .orElseThrow(ChatNotFoundException::new);

        if (!metrics.recordRequestDuration(
                "database", "subscriptions", () -> subscriptionRepository.exists(link.id(), chatId))) {
            throw new ChatNotFoundException();
        }

        metrics.recordRequestDuration(
                "database", "subscriptions", () -> subscriptionRepository.remove(chatId, link.id()));
        metrics.recordRequestDuration("database", "links", () -> linkRepository.removeIfOrphan(link.id()));

        return mapToResponse(link, List.of());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "links", key = "#chatId")
    public ListLinksResponse getLinks(Long chatId) {
        if (!metrics.recordRequestDuration("database", "chats", () -> chatRepository.exists(chatId))) {
            throw new ChatNotFoundException();
        }

        int size = dbProperties.getDefaultPageSize();
        int page = 0;
        List<Subscription> allSubscriptions = new ArrayList<>();
        List<Subscription> batch;

        do {
            int currentPage = page;
            batch = metrics.recordRequestDuration(
                    "database",
                    "subscriptions",
                    () -> subscriptionRepository.findSubscriptionByChatId(chatId, currentPage, size));
            allSubscriptions.addAll(batch);
            page++;
        } while (batch.size() == size);

        List<Long> linkIds = allSubscriptions.stream().map(Subscription::linkId).toList();
        Map<Long, Link> linksMap =
                metrics.recordRequestDuration("database", "links", () -> linkRepository.findByIds(linkIds)).stream()
                        .collect(Collectors.toMap(Link::id, link -> link));

        List<LinkResponse> responseLinks = allSubscriptions.stream()
                .map(sub -> mapToResponse(linksMap.get(sub.linkId()), sub.tags()))
                .toList();

        ListLinksResponse response = new ListLinksResponse();
        response.setLinks(responseLinks);
        response.setSize(responseLinks.size());

        return response;
    }

    @Transactional(readOnly = true)
    public List<Link> getStaleLinks(Instant threshold, int page, int size) {
        return metrics.recordRequestDuration(
                "database", "links", () -> linkRepository.findStaleLinks(threshold, page, size));
    }

    @Transactional
    public void updateLastChecked(Long id, Instant time) {
        metrics.recordRequestDuration("database", "links", () -> linkRepository.updateLastChecked(id, time));
    }

    private LinkResponse mapToResponse(Link link, List<String> tags) {
        LinkResponse response = new LinkResponse();
        response.setId(link.id());
        response.setUrl(link.url());
        response.setTags(tags);
        return response;
    }
}
