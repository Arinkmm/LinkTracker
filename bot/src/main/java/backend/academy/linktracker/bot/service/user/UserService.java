package backend.academy.linktracker.bot.service.user;

import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.repository.UrlRepository;
import backend.academy.linktracker.bot.repository.UserStateRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserStateRepository userStateRepository;
    private final UrlRepository urlRepository;

    public void saveState(Long id, State state) {
        userStateRepository.save(id, state);
    }

    public Optional<State> findStateById(Long id) {
        return userStateRepository.findById(id);
    }

    public Optional<String> findUrlById(Long id) {
        return urlRepository.findById(id);
    }

    public void saveUrl(Long id, String url) {
        urlRepository.save(id, url);
    }

    public void deleteState(Long id) {
        userStateRepository.delete(id);
    }

    public void deleteUrl(Long id) {
        urlRepository.delete(id);
    }
}
