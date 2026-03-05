package backend.academy.linktracker.scrapper.service.notifier;

import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BotNotifier {
    private final BotClient botClient;

    public BotNotifier(BotClient botClient) {
        this.botClient = botClient;
    }

    public void notify(Long chatId, String url, String description, List<Long> tgChatIds) {
        botClient.sendUpdate(new LinkUpdate(chatId, url, description, tgChatIds));
    }
}
