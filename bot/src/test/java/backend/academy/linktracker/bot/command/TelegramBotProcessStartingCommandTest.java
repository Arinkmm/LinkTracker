package backend.academy.linktracker.bot.command;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.impl.HelpCommand;
import backend.academy.linktracker.bot.commands.impl.StartCommand;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import backend.academy.linktracker.bot.service.handler.UpdateHandler;
import backend.academy.linktracker.bot.service.handler.command.CommandHandler;
import backend.academy.linktracker.bot.service.handler.command.UnknownCommandHandler;
import backend.academy.linktracker.bot.service.handler.state.StateHandler;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.MessageValidator;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TelegramBotProcessStartingCommandTest {
    private UpdateHandler handler;

    @Mock
    private TelegramSender telegramSender;

    @Mock
    private CommandProperties commandProperties;

    @Mock
    private MessagesProperties messagesProperties;

    @Mock
    private UserService userService;

    @Mock
    private StateHandler stateHandler;

    @Mock
    private ScrapperClient scrapperClient;

    private final Long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        when(messagesProperties.getWelcome()).thenReturn("Добро пожаловать!");
        when(messagesProperties.getHelpHeader()).thenReturn("Доступные команды:");
        when(messagesProperties.getUnknownCommand()).thenReturn("Неизвестная команда");

        var startInfo = new CommandProperties.CommandInfo();
        startInfo.setName("/start");
        var helpInfo = new CommandProperties.CommandInfo();
        helpInfo.setName("/help");

        var commandsMap = new HashMap<String, CommandProperties.CommandInfo>();
        commandsMap.put(InternalCommand.START.configKey, startInfo);
        commandsMap.put(InternalCommand.HELP.configKey, helpInfo);
        when(commandProperties.getCommands()).thenReturn(commandsMap);

        Command start = new StartCommand(commandProperties, scrapperClient, telegramSender, messagesProperties);
        Command help = new HelpCommand(commandProperties, telegramSender, messagesProperties);

        CommandRegistry registry = new CommandRegistry(List.of(start, help));

        CommandHandler commandHandler = new CommandHandler(registry, userService, telegramSender);
        UnknownCommandHandler unknownCommandHandler = new UnknownCommandHandler(telegramSender, messagesProperties);
        MessageValidator messageValidator = new MessageValidator();

        handler = new UpdateHandler(messageValidator, commandHandler, stateHandler, unknownCommandHandler);

        when(stateHandler.hasState(anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("Успешный /start: регистрация и приветствие")
    void startScenario() {
        Update update = createStubUpdate("/start", CHAT_ID);
        String welcome = messagesProperties.getWelcome();

        handler.process(update);

        verify(scrapperClient).registerChat(CHAT_ID);
        verify(telegramSender).sendMessage(eq(CHAT_ID), eq(welcome));
    }

    @Test
    @DisplayName("Вызов /help возвращает заголовок помощи")
    void helpScenario() {
        Update update = createStubUpdate("/help", CHAT_ID);
        String helpHeader = messagesProperties.getHelpHeader();
        String welcome = messagesProperties.getWelcome();

        handler.process(update);

        verify(telegramSender).sendMessage(eq(CHAT_ID), contains(helpHeader));
        verify(telegramSender, never()).sendMessage(eq(CHAT_ID), eq(welcome));
    }

    @Test
    @DisplayName("Ввод неизвестного текста вызывает UnknownCommandHandler")
    void unknownCommandScenario() {
        Update update = createStubUpdate("просто текст", CHAT_ID);
        String unknown = messagesProperties.getUnknownCommand();

        handler.process(update);

        verify(telegramSender).sendMessage(eq(CHAT_ID), eq(unknown));
    }

    private Update createStubUpdate(String text, Long chatId) {
        String json = """
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 1,
                    "chat": { "id": %d, "type": "private" },
                    "text": "%s"
                  }
                }
                """.formatted(chatId, text);
        return BotUtils.parseUpdate(json);
    }
}
