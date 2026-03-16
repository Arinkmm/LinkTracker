package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.time.Instant;
import java.util.List;

public interface LinkRepository {
    LinkDto getOrCreate(URI url, List<String> tags, List<String> filters);

    LinkDto findById(Long id);

    LinkDto findByUrl(URI url);

    void remove(Long id);

    List<LinkDto> findAll();

    void updateLastChecked(Long id, Instant newTime);
}
