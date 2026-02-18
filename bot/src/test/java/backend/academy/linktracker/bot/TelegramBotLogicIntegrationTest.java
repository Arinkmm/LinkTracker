package backend.academy.linktracker.bot;

import backend.academy.linktracker.bot.commands.Command;
import backend.academy.linktracker.bot.commands.HelpCommand;
import backend.academy.linktracker.bot.commands.StartCommand;
import backend.academy.linktracker.bot.repository.InMemoryUserRepository;
import backend.academy.linktracker.bot.service.UpdateHandler;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

class TelegramBotLogicIntegrationTest {
    private UpdateHandler handler;
    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();

        Command start = new StartCommand(userRepository);
        Command help = new HelpCommand(List.of(start));

        handler = new UpdateHandler(List.of(start, help));
    }

    @Test
    @DisplayName("/start регистрирует пользователя")
    void handleStart() {
        Update update = createStubUpdate("/start", 123L);

        SendMessage result = handler.process(update);
        String text = (String) result.getParameters().get("text");

        Assertions.assertTrue(text.contains("Добро пожаловать"));
        Assertions.assertTrue(userRepository.exists(123L));
    }

    @Test
    @DisplayName("/help выводит список команд")
    void handleHelp() {
        Update update = createStubUpdate("/help", 123L);

        SendMessage result = handler.process(update);
        String text = (String) result.getParameters().get("text");

        Assertions.assertTrue(text.contains("/start"));
    }

    @Test
    @DisplayName("Неизвестная команда")
    void handleUnknown() {
        Update update = createStubUpdate("какой-то текст", 123L);

        SendMessage result = handler.process(update);
        String text = (String) result.getParameters().get("text");

        Assertions.assertTrue(text.contains("Неизвестная команда"));
    }

    private Update createStubUpdate(String text, Long chatId) {
        return new Update() {
            @Override
            public Message message() {
                return new Message() {
                    @Override
                    public String text() { return text; }
                    @Override
                    public Chat chat() {
                        return new Chat() {
                            @Override
                            public Long id() { return chatId; }
                        };
                    }
                };
            }
        };
    }
}
