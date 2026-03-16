package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, LinkDto> links = new ConcurrentHashMap<>();
    private final Map<URI, Long> urlToId = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public LinkDto getOrCreate(URI url, List<String> tags, List<String> filters) {
        Long id = urlToId.computeIfAbsent(url, u -> {
            long newId = idGenerator.getAndIncrement();
            links.put(newId, new LinkDto(newId, u, tags, filters, null));
            return newId;
        });
        return links.get(id);
    }

    @Override
    public LinkDto findById(Long id) {
        return links.get(id);
    }

    @Override
    public LinkDto findByUrl(URI url) {
        Long id = urlToId.get(url);
        return id != null ? links.get(id) : null;
    }

    @Override
    public void remove(Long id) {
        LinkDto removed = links.remove(id);
        if (removed != null) urlToId.remove(removed.url());
    }

    @Override
    public List<LinkDto> findAll() {
        return List.copyOf(links.values());
    }

    @Override
    public void updateLastChecked(Long id, Instant newTime) {
        links.computeIfPresent(id, (k, v) -> v.withLastChecked(newTime));
    }
}
