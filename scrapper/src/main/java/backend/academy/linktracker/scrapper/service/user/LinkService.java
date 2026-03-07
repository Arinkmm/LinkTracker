package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.dto.bot.LinkDto;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyTrackedException;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    public LinkResponse addLink(Long id, AddLinkRequest request) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        if (linkRepository.exists(id, request.link())) {
            throw new LinkAlreadyTrackedException();
        }
        LinkDto saved = linkRepository.save(
            id,
            new LinkDto(id, request.link(), request.tags(), request.filters(), Instant.EPOCH)
        );
        return new LinkResponse(saved.id(), saved.url(), saved.tags(), saved.filters());
    }

    public LinkResponse removeLink(Long id, RemoveLinkRequest request) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        if (!linkRepository.exists(id, request.link())) {
            throw new ChatNotFoundException();
        }
        LinkDto deleted = linkRepository.delete(id, request.link());
        return new LinkResponse(deleted.id(), deleted.url(), deleted.tags(), deleted.filters());
    }

    public ListLinksResponse getLinks(Long id) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        List<LinkResponse> links = linkRepository.findByUserId(id).stream()
            .map(l -> new LinkResponse(l.id(), l.url(), l.tags(), l.filters()))
            .toList();
        return new ListLinksResponse(links, links.size());
    }
}
