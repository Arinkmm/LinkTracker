package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.CommandProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.handler.state.TrackStateHandler;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.TagsParser;
import backend.academy.linktracker.bot.util.UrlValidator;
import java.util.List;
import java.util.Optional;
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
class TrackCommandTest {
    @Mock
    private UserService userService;

    @Mock
    private TelegramSender telegramSender;

    @Mock
    private ScrapperClient scrapperClient;

    @Mock
    private CommandProperties properties;

    @Mock
    private UrlValidator urlValidator;

    @Mock
    private TagsParser tagsParser;

    @InjectMocks
    private TrackStateHandler trackStateHandler;

    @BeforeEach
    void setUp() {
        CommandProperties.Messages messages = mock(CommandProperties.Messages.class);
        when(properties.getMessages()).thenReturn(messages);
    }

    @Test
    @DisplayName("Сценарий 1: Корректная ссылка и теги -> Сохранение")
    void track_CorrectUrlAndTags_SavesToScrapper() {
        Long userId = 1L;
        String url = "https://github.com/user/repo";
        String tagsText = "java, spring";
        List<String> parsedTags = List.of("java", "spring");

        when(urlValidator.isValid(url)).thenReturn(true);
        trackStateHandler.handle(userId, url, State.WAITING_URL);

        verify(userService).saveUrl(userId, url);
        verify(userService).saveState(userId, State.NEEDED_TAGS);

        trackStateHandler.handle(userId, "да", State.NEEDED_TAGS);
        verify(userService).saveState(userId, State.WAITING_TAGS);

        when(userService.findUrlById(userId)).thenReturn(Optional.of(url));
        when(tagsParser.parseTags(tagsText)).thenReturn(parsedTags);

        trackStateHandler.handle(userId, tagsText, State.WAITING_TAGS);

        verify(scrapperClient).addLink(eq(userId), eq(url), eq(parsedTags), any());
        verify(userService).deleteUrl(userId);
        verify(userService).deleteState(userId);
    }

    @Test
    @DisplayName("Сценарий 2: Некорректная ссылка -> Уведомление об ошибке")
    void track_InvalidUrl_NotifiesUser() {
        Long userId = 1L;
        String invalidUrl = "tbank://github.com/user/repo";

        when(urlValidator.isValid(invalidUrl)).thenReturn(false);
        when(properties.getMessages().getInvalidUrl()).thenReturn("Ссылка некорректна");

        trackStateHandler.handle(userId, invalidUrl, State.WAITING_URL);

        verify(telegramSender).sendMessage(userId, "Ссылка некорректна");
        verify(userService, never()).saveUrl(anyLong(), anyString());
    }

    @Test
    @DisplayName("Сценарий 3: Ссылка уже отслеживается -> Ошибка ApiException")
    void track_AlreadyTracked_ThrowsException() {
        Long userId = 1L;
        String url = "https://github.com/user/repo";

        when(userService.findUrlById(userId)).thenReturn(Optional.of(url));

        ApiErrorResponse error =
                new ApiErrorResponse("Conflict", "409", "Test error", "Вы уже подписаны на эту ссылку", List.of());
        doThrow(new ApiException(error)).when(scrapperClient).addLink(any(), any(), any(), any());

        assertThrows(ApiException.class, () -> trackStateHandler.handle(userId, "нет", State.NEEDED_TAGS));
    }
}
