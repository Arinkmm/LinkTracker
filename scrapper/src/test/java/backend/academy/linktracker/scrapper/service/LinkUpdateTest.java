package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.configuration.ExternalApiIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = "app.scheduler.interval=3600000")
class LinkUpdateTest extends ExternalApiIntegrationEnvironment {
    @Autowired private LinkChecker linkChecker;
    @Autowired private LinkRepository linkRepository;
    @Autowired private LinkService linkService;
    @Autowired private ChatRepository chatRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @MockitoBean private BotNotifier botNotifier;

    @Test
    @DisplayName("GitHub: новый Issue -> обрезка превью до 200 симв.")
    void githubUpdate_ShouldTruncatePreview() {
        long chatId = 42L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://github.com/user/repo"));
        subscriptionRepository.save(chatId, link.id(), List.of());

        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        String longBody = "Bug details. ".repeat(30);
        stubGitHub("user", "repo", 200, """
            [ {
                "title": "Bug found",
                "user": { "login": "tester" },
                "body": "%s",
                "created_at": "%s"
            } ]
            """.formatted(longBody, Instant.now()));

        linkChecker.checkAllLinks();

        ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
        verify(botNotifier, timeout(5000)).notify(eq(link.id()), any(), descCaptor.capture(), any());

        String message = descCaptor.getValue();
        assertAll(
            () -> assertTrue(message.contains("..."), "Должно быть многоточие"),
            () -> assertTrue(message.length() < 400, "Сообщение слишком длинное")
        );
    }

    @Test
    @DisplayName("StackOverflow: новый ответ")
    void stackOverflow_ShouldNotify() {
        long chatId = 10L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://stackoverflow.com/questions/111"));
        subscriptionRepository.save(chatId, link.id(), List.of());

        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        long now = Instant.now().getEpochSecond();
        stubStackOverflow("111", 200, """
            { "items": [ {
                "question_id": 111,
                "last_activity_date": %d,
                "answers": [ { "owner": {"display_name": "pro"}, "creation_date": %d } ]
            } ] }
            """.formatted(now, now));

        linkChecker.checkAllLinks();

        verify(botNotifier, timeout(5000)).notify(eq(link.id()), any(), anyString(), any());
    }

    @Test
    @DisplayName("Изоляция ошибок: один API лежит (503), другие ссылки обрабатываются")
    void errorIsolationTest() {
        Link okLink = linkRepository.save(URI.create("https://github.com/user/ok"));
        Link failLink = linkRepository.save(URI.create("https://github.com/user/fail"));

        long chatId = 100L;
        chatRepository.save(chatId);
        subscriptionRepository.save(chatId, okLink.id(), List.of());
        subscriptionRepository.save(chatId, failLink.id(), List.of());

        linkService.updateLastChecked(okLink.id(), Instant.now().minus(1, ChronoUnit.DAYS));
        linkService.updateLastChecked(failLink.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        stubGitHub("user", "ok", 200, "[{\"title\":\"Ok\",\"user\":{\"login\":\"u\"},\"created_at\":\""+Instant.now()+"\"}]");
        stubGitHub("user", "fail", 503, "Unavailable");

        assertDoesNotThrow(() -> linkChecker.checkAllLinks());

        verify(botNotifier, timeout(5000)).notify(eq(okLink.id()), any(), any(), any());

        verify(botNotifier, never()).notify(eq(failLink.id()), any(), any(), any());
    }
}
