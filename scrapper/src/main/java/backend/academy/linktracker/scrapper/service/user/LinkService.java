package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;

    public LinkResponse addLink(Long chatId, AddLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        LinkDto link = linkRepository.getOrCreate(request.getLink(), request.getTags(), request.getFilters());

        if (subscriptionRepository.exists(chatId, link.id())) {
            throw new LinkAlreadyTrackedException();
        }

        subscriptionRepository.add(chatId, link.id());

        return mapToResponse(link);
    }

    public LinkResponse removeLink(Long chatId, RemoveLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        LinkDto link = linkRepository.findByUrl(request.getLink());
        if (link == null || !subscriptionRepository.exists(chatId, link.id())) {
            throw new ChatNotFoundException();
        }

        subscriptionRepository.remove(chatId, link.id());

        if (!subscriptionRepository.hasSubscribers(link.id())) {
            linkRepository.remove(link.id());
        }

        return mapToResponse(link);
    }

    public ListLinksResponse getLinks(Long chatId) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }

        List<LinkResponse> links = subscriptionRepository.findLinksByUser(chatId).stream()
                .map(linkRepository::findById)
                .map(this::mapToResponse)
                .toList();

        ListLinksResponse response = new ListLinksResponse();
        response.setLinks(links);
        response.setSize(links.size());

        return response;
    }

    private LinkResponse mapToResponse(LinkDto dto) {
        LinkResponse response = new LinkResponse();
        response.setId(dto.id());
        response.setUrl(dto.url());
        response.setTags(dto.tags());
        response.setFilters(dto.filters());
        return response;
    }
}
