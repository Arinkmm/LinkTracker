package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListCommandTest {
    @Mock private ScrapperClient scrapperClient;
    @Mock private TelegramSender telegramSender;
    @Mock private CommandProperties commandProperties;

    @InjectMocks
    private CommandExecutor commandExecutor;

    private final Long chatId = 12345L;

    @BeforeEach
    void setUp() {
        CommandProperties.Messages messages = mock(CommandProperties.Messages.class);
        when(commandProperties.getMessages()).thenReturn(messages);

        when(messages.getLinkIsEmpty()).thenReturn("Ссылок не обнаружено");
        when(messages.getLinks()).thenReturn("Ваши ссылки: ");
    }

    @Test
    @DisplayName("Сценарий: /list при наличии подписок -> вывод списка")
    void executeList_WithSubscriptions_SendsFormattedList() {
        LinkResponse link1 = new LinkResponse(1L, "https://github.com/user/repo1", List.of("java"), List.of());
        LinkResponse link2 = new LinkResponse(2L, "https://github.com/user/repo2", List.of("go"), List.of());
        when(scrapperClient.getLinks(chatId)).thenReturn(new ListLinksResponse(List.of(link1, link2), 2));

        commandExecutor.executeList(chatId, null);

        verify(telegramSender).sendMessage(eq(chatId), contains("Ваши ссылки:"));
        verify(telegramSender).sendMessage(eq(chatId), contains("https://github.com/user/repo1"));
        verify(telegramSender).sendMessage(eq(chatId), contains("https://github.com/user/repo2"));
    }

    @Test
    @DisplayName("Сценарий: /list без подписок -> сообщение о пустом списке")
    void executeList_NoSubscriptions_SendsEmptyMessage() {
        when(scrapperClient.getLinks(chatId)).thenReturn(new ListLinksResponse(List.of(), 0));

        commandExecutor.executeList(chatId, null);
        verify(telegramSender).sendMessage(chatId, "Ссылок не обнаружено");
    }

    @Test
    @DisplayName("Сценарий: /list <tag> -> фильтрация списка по тегу")
    void executeList_WithTagFilter_SendsOnlyMatchingLinks() {
        LinkResponse javaLink = new LinkResponse(1L, "https://github.com/user/java-repo", List.of("java"), List.of());
        LinkResponse goLink = new LinkResponse(2L, "https://github.com/user/go-repo", List.of("go"), List.of());
        when(scrapperClient.getLinks(chatId)).thenReturn(new ListLinksResponse(List.of(javaLink, goLink), 2));

        commandExecutor.executeList(chatId, "java");

        verify(telegramSender).sendMessage(eq(chatId), contains("java"));
        verify(telegramSender, never()).sendMessage(eq(chatId), contains("go"));
    }
}
