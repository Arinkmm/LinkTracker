package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.*;
import backend.academy.linktracker.scrapper.exception.*;
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

    @Transactional
    @CacheEvict(value = "links", key = "#chatId")
    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        Link link = linkRepository.save(request.getLink());

        if (subscriptionRepository.exists(link.id(), chatId)) {
            throw new LinkAlreadyTrackedException();
        }

        subscriptionRepository.save(chatId, link.id(), request.getTags());
        return mapToResponse(link, request.getTags());
    }

    @Transactional
    @CacheEvict(value = "links", key = "#chatId")
    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        Link link = linkRepository.findByUrl(request.getLink()).orElseThrow(ChatNotFoundException::new);

        if (!subscriptionRepository.exists(link.id(), chatId)) {
            throw new ChatNotFoundException();
        }

        subscriptionRepository.remove(chatId, link.id());
        linkRepository.removeIfOrphan(link.id());

        return mapToResponse(link, List.of());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "links", key = "#chatId")
    public ListLinksResponse getLinks(Long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        int size = dbProperties.getDefaultPageSize();
        int page = 0;
        List<Subscription> allSubscriptions = new ArrayList<>();
        List<Subscription> batch;

        do {
            batch = subscriptionRepository.findSubscriptionByChatId(chatId, page, size);
            allSubscriptions.addAll(batch);
            page++;
        } while (batch.size() == size);

        List<Long> linkIds = allSubscriptions.stream().map(Subscription::linkId).toList();
        Map<Long, Link> linksMap =
                linkRepository.findByIds(linkIds).stream().collect(Collectors.toMap(Link::id, link -> link));

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
        return linkRepository.findStaleLinks(threshold, page, size);
    }

    @Transactional
    public void updateLastChecked(Long id, Instant time) {
        linkRepository.updateLastChecked(id, time);
    }

    private LinkResponse mapToResponse(Link link, List<String> tags) {
        LinkResponse response = new LinkResponse();
        response.setId(link.id());
        response.setUrl(link.url());
        response.setTags(tags);
        return response;
    }
}
