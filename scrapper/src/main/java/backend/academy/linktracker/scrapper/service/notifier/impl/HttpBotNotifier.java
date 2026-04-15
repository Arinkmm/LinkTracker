package backend.academy.linktracker.scrapper.service.notifier.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpBotNotifier implements BotNotifier {
    private final BotClient botClient;

    public void notify(Long id, URI url, String description, List<Long> tgChatIds) {
        log.atInfo()
                .addKeyValue("id", id)
                .addKeyValue("url", url)
                .addKeyValue("chat_count", tgChatIds.size())
                .log("Notifying bot about link update");

        LinkUpdate update = new LinkUpdate();

        update.setId(id);
        update.setUrl(url);
        update.setDescription(description);
        update.setTgChatIds(tgChatIds);

        botClient.sendUpdate(update);
    }
}
