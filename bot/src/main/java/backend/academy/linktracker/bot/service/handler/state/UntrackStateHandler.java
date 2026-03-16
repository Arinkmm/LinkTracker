package backend.academy.linktracker.bot.service.handler.state;

import static backend.academy.linktracker.bot.model.State.WAITING_UNTRACKING_URL;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UntrackStateHandler implements StateProcessor {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ScrapperClient client;
    private final MessagesProperties properties;

    public void handle(Long id, String text, State state) {
        log.atInfo().addKeyValue("id", id).addKeyValue("url", text).log("Removing link from scrapper");
        URI uri = URI.create(text);
        ListLinksResponse response = client.getLinks(id);
        boolean exists = response != null
                && response.getLinks() != null
                && response.getLinks().size() > 0;
        if (!exists) {
            telegramSender.sendMessage(id, properties.getLinkNotFound());
            return;
        }
        client.removeLink(id, uri);
        userService.deleteState(id);
        telegramSender.sendMessage(id, properties.getLinkDeleted());
    }

    @Override
    public List<State> getSupportedStates() {
        return List.of(WAITING_UNTRACKING_URL);
    }
}
