package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import java.net.URI;
import java.util.List;

public interface BotNotifier {
    void notify(LinkUpdate linkUpdate);
}
