package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.bot.dto.*;
import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScrapperService {
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;

    public void registerChat(long chatId) {
        if (userRepository.exists(chatId)) {
            throw new ChatAlreadyExistsException();
        }
        userRepository.save(chatId);
    }

    public void deleteChat(long chatId) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }
        linkRepository.deleteAllByUserId(chatId);
        userRepository.delete(chatId);
    }

    public LinkResponse addLink(long chatId, AddLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }
        if (linkRepository.exists(chatId, request.link())) {
            throw new LinkAlreadyTrackedException();
        }
        LinkDto saved = linkRepository.save(
            chatId,
            new LinkDto(null, request.link(), request.tags(), request.filters(), Instant.EPOCH, chatId)
        );
        return new LinkResponse(saved.id(), saved.url(), saved.tags(), saved.filters());
    }

    public LinkResponse removeLink(long chatId, RemoveLinkRequest request) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }
        if (!linkRepository.exists(chatId, request.link())) {
            throw new ChatNotFoundException();
        }
        LinkDto deleted = linkRepository.delete(chatId, request.link());
        return new LinkResponse(deleted.id(), deleted.url(), deleted.tags(), deleted.filters());
    }

    public ListLinksResponse getLinks(long chatId) {
        if (!userRepository.exists(chatId)) {
            throw new ChatNotFoundException();
        }
        List<LinkResponse> links = linkRepository.findByUserId(chatId).stream()
            .map(l -> new LinkResponse(l.id(), l.url(), l.tags(), l.filters()))
            .toList();
        return new ListLinksResponse(links, links.size());
    }
}
