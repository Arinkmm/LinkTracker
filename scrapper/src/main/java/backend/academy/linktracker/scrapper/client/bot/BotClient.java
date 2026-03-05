package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;

public interface BotClient {
    void sendUpdate(LinkUpdate linkUpdate);
}
