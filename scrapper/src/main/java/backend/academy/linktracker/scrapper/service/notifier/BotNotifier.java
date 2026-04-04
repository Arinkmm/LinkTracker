package backend.academy.linktracker.scrapper.service.notifier;

import java.net.URI;
import java.util.List;

public interface BotNotifier {
    void notify(Long id, URI url, String description, List<Long> tgChatIds);
}
