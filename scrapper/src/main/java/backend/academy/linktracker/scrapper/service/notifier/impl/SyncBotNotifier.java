package backend.academy.linktracker.scrapper.service.notifier.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class SyncBotNotifier implements BotNotifier {
    private final BotClient botClient;

    public void notify(LinkUpdate linkUpdate) {
        log.atInfo()
                .addKeyValue("id", linkUpdate.getId())
                .addKeyValue("url", linkUpdate.getUrl())
                .addKeyValue("chat_count", linkUpdate.getTgChatIds().size())
                .log("Notifying bot about link update");
        botClient.sendUpdate(linkUpdate);
    }
}
