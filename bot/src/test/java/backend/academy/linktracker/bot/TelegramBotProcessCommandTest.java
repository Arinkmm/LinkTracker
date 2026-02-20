package backend.academy.linktracker.bot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.HelpCommand;
import backend.academy.linktracker.bot.commands.StartCommand;
import backend.academy.linktracker.bot.repository.UserRepository;
import backend.academy.linktracker.bot.service.UpdateHandler;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.utility.BotUtils;
import java.util.List;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramBotProcessCommandTest implements WithAssertions {
    private UpdateHandler handler;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        Command start = new StartCommand(userRepository);
        Command help = new HelpCommand(List.of(start));

        handler = new UpdateHandler(List.of(start, help));
    }

    @Test
    @DisplayName("Успешный /start для нового пользователя")
    void startScenario() {
        Long chatId = 123L;
        Update update = createStubUpdate("/start", chatId);
        when(userRepository.exists(chatId)).thenReturn(false);

        SendMessage result = handler.process(update);

        assertThat(result.getParameters().get("text").toString()).contains("Добро пожаловать");
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Вызов /help возвращает описание команд")
    void helpScenario() {
        Update update = createStubUpdate("/help", 123L);

        SendMessage result = handler.process(update);

        String text = result.getParameters().get("text").toString();
        assertThat(text).contains("Доступные команды");
        assertThat(text).contains("/start");
    }

    @Test
    @DisplayName("Ввод неизвестной команды")
    void unknownCommandScenario() {
        Update update = createStubUpdate("какой-то текст", 123L);

        SendMessage result = handler.process(update);

        assertThat(result.getParameters().get("text").toString()).contains("Неизвестная команда");
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
