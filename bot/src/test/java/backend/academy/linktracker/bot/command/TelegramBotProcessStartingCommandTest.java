package backend.academy.linktracker.bot.command;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.impl.HelpCommand;
import backend.academy.linktracker.bot.commands.impl.StartCommand;
import backend.academy.linktracker.bot.model.InternalCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import backend.academy.linktracker.bot.service.handler.UpdateHandler;
import backend.academy.linktracker.bot.service.handler.command.CommandHandler;
import backend.academy.linktracker.bot.service.handler.command.UnknownCommandHandler;
import backend.academy.linktracker.bot.service.handler.state.StateHandler;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.MessageValidator;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import java.util.List;
import java.util.Map;
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
    private CommandProperties.Messages messages;

    @Mock
    private UserService userService;

    @Mock
    private Map<String, CommandProperties.CommandInfo> commandsMap;

    @Mock
    private CommandProperties.CommandInfo startInfo;

    @Mock
    private CommandProperties.CommandInfo helpInfo;

    @Mock
    private StateHandler stateHandler;

    @Mock
    private CommandExecutor commandExecutor;

    private final Long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        when(messages.getWelcome())
                .thenReturn("Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды");
        when(messages.getHelpHeader()).thenReturn("Доступные команды:");
        when(messages.getUnknownCommand())
                .thenReturn("Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд");

        when(commandProperties.getMessages()).thenReturn(messages);
        when(commandProperties.getCommands()).thenReturn(commandsMap);

        when(commandsMap.get(InternalCommand.START.configKey)).thenReturn(startInfo);
        when(commandsMap.get(InternalCommand.HELP.configKey)).thenReturn(helpInfo);
        when(startInfo.getName()).thenReturn("/start");
        when(helpInfo.getName()).thenReturn("/help");

        Command start = new StartCommand(commandExecutor, commandProperties);
        Command help = new HelpCommand(commandExecutor, commandProperties);
        CommandRegistry registry = new CommandRegistry(List.of(start, help));

        CommandHandler commandHandler = new CommandHandler(registry, userService, telegramSender);
        UnknownCommandHandler unknownCommandHandler = new UnknownCommandHandler(telegramSender, commandProperties);
        MessageValidator messageValidator = new MessageValidator();

        handler = new UpdateHandler(messageValidator, commandHandler, stateHandler, unknownCommandHandler);

        when(stateHandler.hasState(anyLong())).thenReturn(false);

        doAnswer(invocation -> {
                    telegramSender.sendMessage(CHAT_ID, messages.getWelcome());
                    return null;
                })
                .when(commandExecutor)
                .executeStart(CHAT_ID);

        doAnswer(invocation -> {
                    telegramSender.sendMessage(CHAT_ID, messages.getHelpHeader());
                    return null;
                })
                .when(commandExecutor)
                .executeHelp(CHAT_ID);
    }

    @Test
    @DisplayName("Успешный /start для нового пользователя")
    void startScenario() {
        Update update = createStubUpdate("/start", CHAT_ID);

        handler.process(update);

        verify(telegramSender)
                .sendMessage(
                        argThat(chatId -> chatId.equals(CHAT_ID)), argThat(text -> text.equals(messages.getWelcome())));

        verify(telegramSender, never())
                .sendMessage(anyLong(), argThat(text -> text.contains(messages.getHelpHeader())));
    }

    @Test
    @DisplayName("Вызов /help возвращает описание команд")
    void helpScenario() {
        Update update = createStubUpdate("/help", CHAT_ID);

        handler.process(update);

        verify(telegramSender)
                .sendMessage(
                        argThat(chatId -> chatId.equals(CHAT_ID)),
                        argThat(text -> text.contains(messages.getHelpHeader())));

        verify(telegramSender, never()).sendMessage(anyLong(), argThat(text -> text.contains(messages.getWelcome())));
    }

    @Test
    @DisplayName("Ввод неизвестной команды")
    void unknownCommandScenario() {
        String unknownText = "какой-то текст";
        Update update = createStubUpdate(unknownText, CHAT_ID);

        handler.process(update);

        verify(telegramSender)
                .sendMessage(
                        argThat(chatId -> chatId.equals(CHAT_ID)),
                        argThat(text -> text.equals(messages.getUnknownCommand())));

        verify(telegramSender, never()).sendMessage(anyLong(), argThat(text -> text.contains(messages.getWelcome())));
        verify(telegramSender, never())
                .sendMessage(anyLong(), argThat(text -> text.contains(messages.getHelpHeader())));
    }

    private Update createStubUpdate(String text, Long chatId) {
        String json = """
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 1,
                    "chat": { "id": %d, "type": "private" },
                    "from": { "is_bot": false, "first_name": "Test" },
                    "text": "%s"
                  }
                }
                """.formatted(chatId, text);
        return BotUtils.parseUpdate(json);
    }
}
