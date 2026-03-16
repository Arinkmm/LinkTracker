package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.model.State;
import backend.academy.linktracker.bot.properties.MessagesProperties;
import backend.academy.linktracker.bot.properties.ResponsesProperties;
import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.bot.service.handler.state.TrackStateHandler;
import backend.academy.linktracker.bot.service.user.UserService;
import backend.academy.linktracker.bot.util.TagsParser;
import backend.academy.linktracker.bot.util.url.validator.UrlValidator;
import java.net.URI;
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
    private MessagesProperties messagesProperties;

    @Mock
    private ResponsesProperties responsesProperties;

    @Mock
    private UrlValidator urlValidator;

    @Mock
    private TagsParser tagsParser;

    @InjectMocks
    private TrackStateHandler trackStateHandler;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        when(messagesProperties.getInvalidUrl()).thenReturn("Ссылка некорректна");
        when(messagesProperties.getTagsOffer()).thenReturn("Хотите добавить теги?");
        when(messagesProperties.getTagsPrompt()).thenReturn("Введите теги через запятую");
        when(messagesProperties.getLinkAdded()).thenReturn("Ссылка успешно добавлена!");

        when(responsesProperties.getNeededTags()).thenReturn("да");
        when(responsesProperties.getNotNeededTags()).thenReturn("нет");
    }

    @Test
    @DisplayName("Сценарий 1: Корректная ссылка и теги -> Сохранение")
    void track_CorrectUrlAndTags_SavesToScrapper() {
        String urlString = "https://github.com/user/repo";
        URI uri = URI.create(urlString);
        String tagsText = "java, spring";
        List<String> parsedTags = List.of("java", "spring");

        when(urlValidator.isValid(uri)).thenReturn(true);
        trackStateHandler.handle(userId, urlString, State.WAITING_URL);

        verify(userService).saveUrl(userId, uri);
        verify(userService).saveState(userId, State.NEEDED_TAGS);

        trackStateHandler.handle(userId, responsesProperties.getNeededTags(), State.NEEDED_TAGS);
        verify(userService).saveState(userId, State.WAITING_TAGS);

        when(userService.findUrlById(userId)).thenReturn(Optional.of(uri));
        when(tagsParser.parseTags(tagsText)).thenReturn(parsedTags);

        trackStateHandler.handle(userId, tagsText, State.WAITING_TAGS);

        verify(scrapperClient).addLink(eq(userId), eq(uri), eq(parsedTags), any());
        verify(userService).deleteUrl(userId);
        verify(userService).deleteState(userId);
    }

    @Test
    @DisplayName("Сценарий 2: Некорректная ссылка -> Уведомление об ошибке")
    void track_InvalidUrl_NotifiesUser() {
        String invalidUrl = "bad-url";

        when(urlValidator.isValid(any())).thenReturn(false);

        trackStateHandler.handle(userId, invalidUrl, State.WAITING_URL);

        verify(telegramSender).sendMessage(userId, messagesProperties.getInvalidUrl());
        verify(userService, never()).saveUrl(any(), any());
    }

    @Test
    @DisplayName("Сценарий 3: Ссылка уже отслеживается -> Conflict")
    void track_AlreadyTracked_ThrowsException() {
        URI uri = URI.create("https://github.com/user/repo");
        when(userService.findUrlById(userId)).thenReturn(Optional.of(uri));

        ApiErrorResponse error = new ApiErrorResponse();
        error.setCode("409");

        doThrow(new ApiException(error)).when(scrapperClient).addLink(anyLong(), any(), any(), any());

        assertThrows(
                ApiException.class,
                () -> trackStateHandler.handle(userId, responsesProperties.getNotNeededTags(), State.NEEDED_TAGS));
    }
}
