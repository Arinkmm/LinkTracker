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

    public void saveState(Long userId, State state) {
        userStateRepository.save(userId, state);
    }

    public Optional<State> findStateById(Long userId) {
        return userStateRepository.findById(userId);
    }

    public Optional<String> findUrlById(Long userId) {
        return urlRepository.findById(userId);
    }

    public void saveUrl(Long userId, String url) {
        urlRepository.save(userId, url);
    }

    public void deleteState(Long userId) {
        userStateRepository.delete(userId);
    }

    public void deleteUrl(Long userId) {
        urlRepository.delete(userId);
    }
}
