package backend.academy.linktracker.bot.service.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommandExecutor {
    private final CommandProperties commandProperties;
    private final TelegramSender telegramSender;
    private final UserService userService;
    private final ScrapperClient client;

    public void executeStart(Long id) {
        client.registerChat(id);
        telegramSender.sendMessage(id, commandProperties.getMessages().getWelcome());
    }

    public void executeHelp(Long id) {
        StringBuilder helpText =
            new StringBuilder(commandProperties.getMessages().getHelpHeader());

        commandProperties.getCommands().values().forEach(cmd -> helpText.append("\n- ")
            .append(cmd.getName())
            .append(" — ")
            .append(cmd.getDescription()));

        telegramSender.sendMessage(id, helpText.toString());
    }

    public void executeList(Long id, String tag) {
        List<LinkResponse> links = client.getLinks(id).links();

        if (tag != null) {
            links = links.stream().filter(link -> link.tags().contains(tag)).toList();
        }

        if (links.isEmpty()) {
            telegramSender.sendMessage(id, commandProperties.getMessages().getLinkIsEmpty());
            return;
        }

        StringBuilder list = new StringBuilder(commandProperties.getMessages().getLinks() + "\n");
        for (int i = 0; i < links.size(); i++) {
            LinkResponse link = links.get(i);
            list.append((i + 1))
                .append(". ")
                .append(link.url())
                .append(" [")
                .append(String.join(", ", link.tags()))
                .append("]")
                .append("\n");
        }
        telegramSender.sendMessage(id, list.toString());
    }

    public void executeCancel(Long id) {
        userService.deleteState(id);
        userService.deleteUrl(id);
        telegramSender.sendMessage(id, commandProperties.getMessages().getCanceling());
    }

    public void executeTrack(Long id) {
        userService.saveState(id, State.WAITING_URL);
        telegramSender.sendMessage(id, commandProperties.getMessages().getTracking());
    }

    public void executeUntrack(Long id) {
        userService.saveState(id, State.WAITING_UNTRACKING_URL);
        telegramSender.sendMessage(id, commandProperties.getMessages().getUntracking());
    }

}
