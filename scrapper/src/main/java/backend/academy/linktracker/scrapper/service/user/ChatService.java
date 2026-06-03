package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.metrics.ScrapperMetrics;
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
    private final ScrapperMetrics metrics;

    @Transactional
    public void registerChat(Long id) {
        if (metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_CHATS, () -> chatRepository.exists(id))) {
            throw new ChatAlreadyExistsException();
        }
        metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_CHATS, () -> chatRepository.save(id));
    }

    @Transactional
    @CacheEvict(value = "links", key = "#id")
    public void deleteChat(Long id) {
        if (!metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_CHATS, () -> chatRepository.exists(id))) {
            throw new ChatNotFoundException();
        }

        metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_CHATS, () -> chatRepository.delete(id));
        metrics.recordRequestDuration(
                ScrapperMetrics.SCOPE_DATABASE, ScrapperMetrics.TYPE_LINKS, linkRepository::removeOrphans);
    }
}
