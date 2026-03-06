package backend.academy.linktracker.bot.service.handler.state;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UntrackStateHandler {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ScrapperClient client;
    private final CommandProperties properties;

    public void handle(Long id, String text) {
        log.atInfo()
            .addKeyValue("id", id)
            .addKeyValue("url", text)
            .log("Removing link from scrapper");

        client.removeLink(id, text);
        userService.deleteState(id);
        telegramSender.sendMessage(id, properties.getMessages().getLinkDeleted());
    }
}
