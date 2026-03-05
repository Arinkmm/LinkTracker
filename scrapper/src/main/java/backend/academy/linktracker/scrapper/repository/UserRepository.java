package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.UserDto;
import java.util.List;

public interface UserRepository {
    void save(Long userId);

    boolean exists(Long userId);

    void delete(Long userId);

    List<UserDto> findAll();
}
