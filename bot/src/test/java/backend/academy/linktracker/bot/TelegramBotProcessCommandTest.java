package backend.academy.linktracker.bot;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.HelpCommand;
import backend.academy.linktracker.bot.commands.StartCommand;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandRegistry;
import backend.academy.linktracker.bot.service.update.UpdateHandler;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.utility.BotUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramBotProcessCommandTest {

    private UpdateHandler handler;

    @Mock
    private TelegramSender telegramSender;

    @Mock
    private CommandProperties commandProperties;

    @Mock
    private CommandProperties.Messages messages;

    @Mock
    private CommandProperties.Commands commands;

    @Mock
    private CommandProperties.CommandInfo startInfo;

    @Mock
    private CommandProperties.CommandInfo helpInfo;

    private final Long CHAT_ID = 123L;
    private final Long USER_ID = 456L;

    @BeforeEach
    void setUp() {
        when(messages.getWelcome())
                .thenReturn("Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды");
        when(messages.getHelpHeader()).thenReturn("Доступные команды:");
        when(messages.getUnknownCommand())
                .thenReturn("Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд");

        when(commandProperties.getMessages()).thenReturn(messages);
        when(commandProperties.getCommands()).thenReturn(commands);

        when(commands.getStart()).thenReturn(startInfo);
        when(commands.getHelp()).thenReturn(helpInfo);

        when(startInfo.getName()).thenReturn("/start");
        when(helpInfo.getName()).thenReturn("/help");

        Command start = new StartCommand(telegramSender, commandProperties);
        Command help = new HelpCommand(telegramSender, commandProperties);
        handler = new UpdateHandler(new CommandRegistry(List.of(start, help)), telegramSender, commandProperties);
    }

    @Test
    @DisplayName("Успешный /start для нового пользователя")
    void startScenario() {
        Update update = createStubUpdate(commands.getStart().getName(), CHAT_ID, USER_ID);

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
        Update update = createStubUpdate(commands.getHelp().getName(), CHAT_ID, USER_ID);

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
        Update update = createStubUpdate(unknownText, CHAT_ID, USER_ID);

        handler.process(update);

        verify(telegramSender)
                .sendMessage(
                        argThat(chatId -> chatId.equals(CHAT_ID)),
                        argThat(text -> text.equals(messages.getUnknownCommand())));

        verify(telegramSender, never()).sendMessage(anyLong(), argThat(text -> text.contains(messages.getWelcome())));
        verify(telegramSender, never())
                .sendMessage(anyLong(), argThat(text -> text.contains(messages.getHelpHeader())));
    }

    private Update createStubUpdate(String text, Long chatId, Long userId) {
        String json = """
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 1,
                    "chat": { "id": %d, "type": "private" },
                    "from": { "id": %d, "is_bot": false, "first_name": "Test" },
                    "text": "%s"
                  }
                }
                """.formatted(chatId, userId, text);
        return BotUtils.parseUpdate(json);
    }
}
