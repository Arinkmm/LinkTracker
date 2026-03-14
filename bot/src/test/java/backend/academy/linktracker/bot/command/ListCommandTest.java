package backend.academy.linktracker.bot.command;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.command.CommandExecutor;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ListCommandTest {
    @Mock
    private ScrapperClient scrapperClient;

    @Mock
    private TelegramSender telegramSender;

    @Mock
    private CommandProperties commandProperties;

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
        LinkResponse link1 = new LinkResponse();
        link1.setId(1L);
        link1.setUrl(URI.create("https://github.com/user/repo1"));
        link1.setTags(List.of("java"));

        LinkResponse link2 = new LinkResponse();
        link2.setId(2L);
        link2.setUrl(URI.create("https://github.com/user/repo2"));
        link2.setTags(List.of("go"));

        ListLinksResponse listResponse = new ListLinksResponse();
        listResponse.setLinks(List.of(link1, link2));

        when(scrapperClient.getLinks(chatId)).thenReturn(listResponse);

        commandExecutor.executeList(chatId, null);

        verify(telegramSender).sendMessage(eq(chatId), contains("Ваши ссылки:"));
        verify(telegramSender).sendMessage(eq(chatId), contains("https://github.com/user/repo1"));
        verify(telegramSender).sendMessage(eq(chatId), contains("https://github.com/user/repo2"));
    }

    @Test
    @DisplayName("Сценарий: /list без подписок -> сообщение о пустом списке")
    void executeList_NoSubscriptions_SendsEmptyMessage() {
        ListLinksResponse emptyResponse = new ListLinksResponse();
        emptyResponse.setLinks(List.of());

        when(scrapperClient.getLinks(chatId)).thenReturn(emptyResponse);

        commandExecutor.executeList(chatId, null);
        verify(telegramSender).sendMessage(chatId, "Ссылок не обнаружено");
    }

    @Test
    @DisplayName("Сценарий: /list <tag> -> фильтрация списка по тегу")
    void executeList_WithTagFilter_SendsOnlyMatchingLinks() {
        LinkResponse javaLink = new LinkResponse();
        javaLink.setId(1L);
        javaLink.setUrl(URI.create("https://github.com/user/java-repo"));
        javaLink.setTags(List.of("java"));

        LinkResponse goLink = new LinkResponse();
        goLink.setId(2L);
        goLink.setUrl(URI.create("https://github.com/user/go-repo"));
        goLink.setTags(List.of("go"));

        ListLinksResponse listResponse = new ListLinksResponse();
        listResponse.setLinks(List.of(javaLink, goLink));

        when(scrapperClient.getLinks(chatId)).thenReturn(listResponse);

        commandExecutor.executeList(chatId, "java");

        verify(telegramSender).sendMessage(eq(chatId), contains("java"));
        verify(telegramSender, never()).sendMessage(eq(chatId), contains("go-repo"));
    }
}
