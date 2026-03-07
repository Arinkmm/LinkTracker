package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.dto.bot.LinkDto;
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
    public LinkDto save(Long id, LinkDto linkDto) {
        LinkDto link = new LinkDto(id, linkDto.url(), linkDto.tags(), linkDto.filters(), linkDto.lastChecked());
        map.computeIfAbsent(id, k -> new ArrayList<>()).add(link);
        return link;
    }

    @Override
    public List<LinkDto> findByUserId(Long id) {
        return map.getOrDefault(id, List.of());
    }

    @Override
    public boolean exists(Long id, String url) {
        return findByUserId(id).stream().anyMatch(l -> l.url().equals(url));
    }

    @Override
    public LinkDto delete(Long id, String url) {
        List<LinkDto> links = map.getOrDefault(id, new ArrayList<>());
        LinkDto found =
                links.stream().filter(l -> l.url().equals(url)).findFirst().orElseThrow();
        links.remove(found);
        return found;
    }

    @Override
    public void deleteAllById(Long id) {
        map.remove(id);
    }

    @Override
    public List<LinkDto> findAll() {
        return map.values().stream().flatMap(List::stream).toList();
    }
}
