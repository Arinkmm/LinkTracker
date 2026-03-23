package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.dto.Subscription;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.properties.DBProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DBProperties dbProperties;

    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        Link link = linkRepository.save(request.getLink());

        if (subscriptionRepository.exists(link.id(), chatId)) {
            throw new LinkAlreadyTrackedException();
        }

        subscriptionRepository.save(chatId, link.id(), request.getTags());

        return mapToResponse(link, request.getTags());
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        Link link = linkRepository.findByUrl(request.getLink()).orElseThrow(ChatNotFoundException::new);

        if (!subscriptionRepository.exists(link.id(), chatId)) {
            throw new ChatNotFoundException();
        }

        subscriptionRepository.remove(chatId, link.id());

        if (subscriptionRepository.findUserIdByLinkId(link.id()).isEmpty()) {
            linkRepository.remove(link.id());
        }

        return mapToResponse(link, List.of());
    }

    public ListLinksResponse getLinks(Long chatId) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        int size = dbProperties.getDefaultPageSize();
        int page = 0;
        List<Subscription> allSubscriptions = new ArrayList<>();
        List<Subscription> batch;

        do {
            batch = subscriptionRepository.findSubscriptionByUserId(chatId, page, size);
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

    private LinkResponse mapToResponse(Link link, List<String> tags) {
        LinkResponse response = new LinkResponse();
        response.setId(link.id());
        response.setUrl(link.url());
        response.setTags(tags);
        return response;
    }
}
