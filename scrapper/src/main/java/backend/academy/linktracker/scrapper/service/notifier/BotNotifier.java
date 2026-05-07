package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.bot.dto.LinkUpdate;

public interface BotNotifier {
    void notify(LinkUpdate linkUpdate);
}
