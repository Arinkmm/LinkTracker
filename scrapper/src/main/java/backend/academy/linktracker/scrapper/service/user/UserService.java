package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;

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

        List<Long> userLinkIds = subscriptionRepository.findLinksByUser(id);

        subscriptionRepository.removeUser(id);

        userLinkIds.forEach(linkId -> {
            if (!subscriptionRepository.hasSubscribers(linkId)) {
                linkRepository.remove(linkId);
            }
        });

        userRepository.delete(id);
    }
}
