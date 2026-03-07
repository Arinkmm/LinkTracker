package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.dto.*;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;

    public void registerChat(Long id) {
        if (userRepository.exists(id)) {
            throw new ChatAlreadyExistsException();
        }
        userRepository.save(id);
    }

    public void deleteChat(Long id) {
        if (!userRepository.exists(id)) {
            throw new ChatNotFoundException();
        }
        linkRepository.deleteAllById(id);
        userRepository.delete(id);
    }
}
