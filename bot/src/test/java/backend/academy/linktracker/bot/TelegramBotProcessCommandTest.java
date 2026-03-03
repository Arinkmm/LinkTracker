package backend.academy.linktracker.bot;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
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

    @BeforeEach
    void setUp() {
        when(commandProperties.getMessages()).thenReturn(messages);
        when(commandProperties.getCommands()).thenReturn(commands);
        when(commands.getStart()).thenReturn(startInfo);
        when(commands.getHelp()).thenReturn(helpInfo);
        when(startInfo.getName()).thenReturn("/start");
        when(startInfo.getDescription()).thenReturn("Начать работу");
        when(helpInfo.getName()).thenReturn("/help");
        when(helpInfo.getDescription()).thenReturn("Вывести список доступных команд");
        when(messages.getWelcome())
                .thenReturn("Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды");
        when(messages.getHelpHeader()).thenReturn("Доступные команды:");
        when(messages.getUnknownCommand())
                .thenReturn("Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд");

        Command start = new StartCommand(telegramSender, commandProperties);
        Command help = new HelpCommand(telegramSender, commandProperties);

        handler = new UpdateHandler(new CommandRegistry(List.of(start, help)), telegramSender, commandProperties);
    }

    @Test
    @DisplayName("Успешный /start для нового пользователя")
    void startScenario() {
        Update update = createStubUpdate("/start", 123L, 456L);

        handler.process(update);

        verify(telegramSender).sendMessage(anyLong(), contains("Добро пожаловать"));
    }

    @Test
    @DisplayName("Вызов /help возвращает описание команд")
    void helpScenario() {
        Update update = createStubUpdate("/help", 123L, 456L);

        handler.process(update);

        verify(telegramSender).sendMessage(anyLong(), contains("Доступные команды"));
    }

    @Test
    @DisplayName("Ввод неизвестной команды")
    void unknownCommandScenario() {
        Update update = createStubUpdate("какой-то текст", 123L, 456L);

        handler.process(update);

        verify(telegramSender).sendMessage(anyLong(), contains("Неизвестная команда"));
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
