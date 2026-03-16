package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import com.pengrad.telegrambot.model.Message;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ListCommand extends AbstractCommand {
    private final ScrapperClient client;
    private final TelegramSender telegramSender;
    private final MessagesProperties messagesProperties;

    public ListCommand(
            CommandProperties commandProperties,
            ScrapperClient scrapperClient,
            TelegramSender telegramSender,
            MessagesProperties messagesProperties) {
        super(commandProperties, InternalCommand.LIST.configKey);
        this.client = scrapperClient;
        this.telegramSender = telegramSender;
        this.messagesProperties = messagesProperties;
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        String text = message.text();

        String[] parts = text.split(" ", 2);
        String tag = parts.length > 1 ? parts[1] : null;

        ListLinksResponse response = client.getLinks(id);
        List<LinkResponse> links = (response != null) ? response.getLinks() : null;

        if (tag != null && links != null) {
            links = links.stream()
                    .filter(link -> link.getTags() != null && link.getTags().contains(tag))
                    .toList();
        }

        if (links == null || links.isEmpty()) {
            telegramSender.sendMessage(id, messagesProperties.getLinkIsEmpty());
            return;
        }

        StringBuilder list = new StringBuilder(messagesProperties.getLinks() + "\n");
        for (int i = 0; i < links.size(); i++) {
            LinkResponse link = links.get(i);
            list.append(i + 1)
                    .append(". ")
                    .append(link.getUrl())
                    .append(" [")
                    .append(link.getTags() != null ? String.join(", ", link.getTags()) : "")
                    .append("]")
                    .append("\n");
        }
        telegramSender.sendMessage(id, list.toString());
    }
}
