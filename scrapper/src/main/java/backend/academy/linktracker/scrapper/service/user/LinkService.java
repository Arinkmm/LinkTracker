package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkDto; // Твой внутренний рекорд/класс
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    public LinkResponse addLink(Long id, AddLinkRequest request) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        if (linkRepository.exists(id, request.getLink())) {
            throw new LinkAlreadyTrackedException();
        }

        LinkDto saved =
                linkRepository.save(id, new LinkDto(id, request.getLink(), request.getTags(), null, Instant.EPOCH));

        return mapToResponse(saved);
    }

    public LinkResponse removeLink(Long id, RemoveLinkRequest request) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        if (!linkRepository.exists(id, request.getLink())) {
            throw new ChatNotFoundException();
        }
        LinkDto deleted = linkRepository.delete(id, request.getLink());

        return mapToResponse(deleted);
    }

    public ListLinksResponse getLinks(Long id) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        List<LinkResponse> links = linkRepository.findByUserId(id).stream()
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
