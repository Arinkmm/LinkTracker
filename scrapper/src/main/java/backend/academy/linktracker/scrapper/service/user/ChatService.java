package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    @Transactional
    public void registerChat(Long id) {
        if (chatRepository.exists(id)) {
            throw new ChatAlreadyExistsException();
        }
        chatRepository.save(id);
    }

    @Transactional
    @CacheEvict(value = "links", key = "#id")
    public void deleteChat(Long id) {
        if (!chatRepository.exists(id)) {
            throw new ChatNotFoundException();
        }

        chatRepository.delete(id);
        linkRepository.removeOrphans();
    }
}
