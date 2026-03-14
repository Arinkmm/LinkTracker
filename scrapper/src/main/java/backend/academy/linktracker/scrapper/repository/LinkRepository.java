package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.LinkDto;
import java.net.URI;
import java.util.List;

public interface LinkRepository {
    LinkDto save(Long id, LinkDto linkDto);

    List<LinkDto> findByUserId(Long id);

    boolean exists(Long id, URI url);

    LinkDto delete(Long id, URI url);

    void deleteAllById(Long id);

    List<LinkDto> findAll();
}
