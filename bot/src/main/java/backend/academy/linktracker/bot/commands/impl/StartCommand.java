package backend.academy.linktracker.bot.commands.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import com.pengrad.telegrambot.model.Message;
import org.springframework.stereotype.Component;

@Component
public class StartCommand extends AbstractCommand {
    private final ScrapperClient scrapperClient;
    private final TelegramSender telegramSender;
    private final MessagesProperties messagesProperties;

    public StartCommand(
            CommandProperties commandProperties,
            ScrapperClient scrapperClient,
            TelegramSender telegramSender,
            MessagesProperties messagesProperties) {
        super(commandProperties, InternalCommand.START.configKey);
        this.scrapperClient = scrapperClient;
        this.telegramSender = telegramSender;
        this.messagesProperties = messagesProperties;
    }

    @Override
    public void handle(Message message) {
        Long id = message.chat().id();
        scrapperClient.registerChat(id);
        telegramSender.sendMessage(id, messagesProperties.getWelcome());
    }
}
