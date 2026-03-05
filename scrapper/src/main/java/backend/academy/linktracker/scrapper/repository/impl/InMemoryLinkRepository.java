package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {

    private final Map<Long, List<LinkDto>> map = new ConcurrentHashMap<>();

    @Override
    public LinkDto save(Long userId, LinkDto linkDto) {
        LinkDto link =
                new LinkDto(userId, linkDto.url(), linkDto.tags(), linkDto.filters(), linkDto.lastChecked(), userId);
        map.computeIfAbsent(userId, k -> new ArrayList<>()).add(link);
        return link;
    }

    @Override
    public List<LinkDto> findByUserId(Long userId) {
        return map.getOrDefault(userId, List.of());
    }

    @Override
    public boolean exists(Long userId, String url) {
        return findByUserId(userId).stream().anyMatch(l -> l.url().equals(url));
    }

    @Override
    public LinkDto delete(Long userId, String url) {
        List<LinkDto> links = map.getOrDefault(userId, new ArrayList<>());
        LinkDto found =
                links.stream().filter(l -> l.url().equals(url)).findFirst().orElseThrow();
        links.remove(found);
        return found;
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        map.remove(userId);
    }

    @Override
    public List<LinkDto> findAll() {
        return map.values().stream().flatMap(List::stream).toList();
    }
}
