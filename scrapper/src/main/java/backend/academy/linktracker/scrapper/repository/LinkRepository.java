package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.bot.LinkDto;
import java.util.List;

public interface LinkRepository {
    LinkDto save(Long id, LinkDto linkDto);

    List<LinkDto> findByUserId(Long id);

    boolean exists(Long id, String url);

    LinkDto delete(Long id, String url);

    void deleteAllById(Long id);

    List<LinkDto> findAll();
}
