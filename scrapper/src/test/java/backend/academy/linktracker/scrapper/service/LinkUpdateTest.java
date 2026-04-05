package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.configuration.ExternalApiIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.Link;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
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
    @Autowired
    private LinkChecker linkChecker;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private BotNotifier botNotifier;

    @Test
    @DisplayName("GitHub: сообщение содержит название, автора и обрезанное превью (ровно 200 симв.)")
    void githubUpdate_MessageContainsTitleAuthorAndTruncatedAt200() {
        Long chatId = 43L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://github.com/user/repo2"));
        subscriptionRepository.save(chatId, link.id(), List.of());
        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        String body = "X".repeat(300);
        stubGitHub("user", "repo2", 200, """
            [ {
                "title": "My Issue Title",
                "user": { "login": "author123" },
                "body": "%s",
                "created_at": "%s"
            } ]
            """.formatted(body, Instant.now()));

        linkChecker.checkAllLinks();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(botNotifier, timeout(5000)).notify(eq(link.id()), any(), captor.capture(), any());

        String msg = captor.getValue();
        assertAll(
                () -> assertTrue(msg.contains("My Issue Title"), "Нет названия"),
                () -> assertTrue(msg.contains("author123"), "Нет автора"),
                () -> assertTrue(msg.contains("..."), "Нет обрезки"),
                () -> assertTrue(msg.contains("X".repeat(200)), "Превью должно содержать ровно 200 символов"),
                () -> assertFalse(msg.contains("X".repeat(201)), "Превью не должно превышать 200 символов"));
    }

    @Test
    @DisplayName("StackOverflow: сообщение содержит автора ответа и превью")
    void stackOverflow_MessageContainsAnswerAuthorAndPreview() {
        Long chatId = 11L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://stackoverflow.com/questions/222"));
        subscriptionRepository.save(chatId, link.id(), List.of());
        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        Long now = Instant.now().getEpochSecond();
        stubStackOverflow("222", 200, """
            { "items": [ {
                "question_id": 222,
                "last_activity_date": %d,
                "answers": [ {
                    "owner": {"display_name": "expert_user"},
                    "body": "Here is my detailed answer",
                    "creation_date": %d
                } ]
            } ] }
            """.formatted(now, now));

        linkChecker.checkAllLinks();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(botNotifier, timeout(5000)).notify(eq(link.id()), any(), captor.capture(), any());

        String msg = captor.getValue();
        assertAll(
                () -> assertTrue(msg.contains("expert_user"), "Нет автора ответа"),
                () -> assertTrue(msg.contains("Here is my detailed answer"), "Нет превью ответа"));
    }

    @Test
    @DisplayName("Пакетная обработка: несколько ссылок обрабатываются за один цикл")
    void batchProcessing_MultipleLinksHandledTogether() {
        Long chatId = 200L;
        chatRepository.save(chatId);

        Link link1 = linkRepository.save(URI.create("https://stackoverflow.com/questions/301"));
        Link link2 = linkRepository.save(URI.create("https://stackoverflow.com/questions/302"));
        Link link3 = linkRepository.save(URI.create("https://stackoverflow.com/questions/303"));

        for (Link link : List.of(link1, link2, link3)) {
            subscriptionRepository.save(chatId, link.id(), List.of());
            linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));
        }

        Long now = Instant.now().getEpochSecond();
        for (String id : List.of("301", "302", "303")) {
            stubStackOverflow(id, 200, """
                { "items": [ {
                    "question_id": %s,
                    "last_activity_date": %d,
                    "answers": [ { "owner": {"display_name": "u"}, "creation_date": %d } ]
                } ] }
                """.formatted(id, now, now));
        }

        linkChecker.checkAllLinks();

        verify(botNotifier, timeout(5000)).notify(eq(link1.id()), any(), any(), any());
        verify(botNotifier, timeout(5000)).notify(eq(link2.id()), any(), any(), any());
        verify(botNotifier, timeout(5000)).notify(eq(link3.id()), any(), any(), any());
    }

    @Test
    @DisplayName("API недоступен (503): уведомление не отправляется, приложение не падает")
    void apiUnavailable_BotNotNotified() {
        Long chatId = 300L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://github.com/user/unavailable"));
        subscriptionRepository.save(chatId, link.id(), List.of());
        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        stubGitHub("user", "unavailable", 503, "Service Unavailable");

        assertDoesNotThrow(() -> linkChecker.checkAllLinks());
        verify(botNotifier, never()).notify(eq(link.id()), any(), any(), any());
    }
}
