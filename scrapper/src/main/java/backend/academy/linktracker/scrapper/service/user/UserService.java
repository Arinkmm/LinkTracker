package backend.academy.linktracker.scrapper.service.user;

import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

        List<Long> deletingLinkIds = subscriptionRepository.findLinkIdByUserId(id).stream()
                .filter(linkId ->
                        subscriptionRepository.findUserIdByLinkId(linkId).size() == 1)
                .toList();

        userRepository.delete(id);

        deletingLinkIds.forEach(linkRepository::remove);
    }
}
