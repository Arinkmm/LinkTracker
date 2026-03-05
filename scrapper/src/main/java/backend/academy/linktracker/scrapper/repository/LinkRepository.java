package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.bot.LinkDto;
import java.util.List;

public interface LinkRepository {
    LinkDto save(Long userId, LinkDto linkDto);

    List<LinkDto> findByUserId(Long userId);

    boolean exists(Long userId, String url);

    LinkDto delete(Long userId, String url);

    void deleteAllByUserId(Long userId);

    List<LinkDto> findAll();
}
