package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.configuration.ExternalApiIntegrationEnvironment;
import backend.academy.linktracker.scrapper.configuration.TestDatabaseCleaner;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private BotNotifier botNotifier;

    @BeforeEach
    void setUp() {
        TestDatabaseCleaner.clean(jdbcTemplate);
        reset(botNotifier);
    }

    @Test
    @DisplayName("GitHub Issue: сообщение содержит название, автора и обрезанное превью")
    void githubUpdate_MessageContainsTitleAuthorAndFullBody() {
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

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botNotifier, timeout(5000).atLeastOnce()).notify(captor.capture());

        LinkUpdate update = captor.getAllValues().stream()
                .filter(u -> u.getId().equals(link.id()))
                .findFirst()
                .orElseThrow();

        String msg = update.getDescription();
        assertAll(
                () -> assertTrue(msg.contains("My Issue Title")),
                () -> assertTrue(msg.contains("author123")),
                () -> assertTrue(msg.contains(body)),
                () -> assertFalse(msg.contains("X".repeat(200) + "...")));
    }

    @Test
    @DisplayName("GitHub PR: корректное распознавание нового Pull Request")
    void githubUpdate_HandlesPullRequest() {
        Long chatId = 44L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://github.com/user/pr-repo"));
        subscriptionRepository.save(chatId, link.id(), List.of());
        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        stubGitHub("user", "pr-repo", 200, """
            [ {
                "title": "Add new feature",
                "user": { "login": "developer" },
                "pull_request": { "url": "https://api.github.com/repos/user/pr-repo/pulls/1" },
                "created_at": "%s"
            } ]
            """.formatted(Instant.now()));

        linkChecker.checkAllLinks();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botNotifier, timeout(5000).atLeastOnce()).notify(captor.capture());

        String msg = captor.getAllValues().stream()
                .filter(u -> u.getId().equals(link.id()))
                .findFirst()
                .orElseThrow()
                .getDescription();

        assertTrue(msg.toLowerCase().contains("pull request") || msg.contains("PR"));
    }

    @Test
    @DisplayName("StackOverflow Answer: сообщение содержит автора ответа и превью")
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

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botNotifier, timeout(5000).atLeastOnce()).notify(captor.capture());

        String msg = captor.getAllValues().stream()
                .filter(u -> u.getId().equals(link.id()))
                .findFirst()
                .orElseThrow()
                .getDescription();

        assertAll(
                () -> assertTrue(msg.contains("expert_user")),
                () -> assertTrue(msg.contains("Here is my detailed answer")));
    }

    @Test
    @DisplayName("StackOverflow Comment: сообщение содержит автора комментария")
    void stackOverflow_HandlesNewComment() {
        Long chatId = 12L;
        chatRepository.save(chatId);
        Link link = linkRepository.save(URI.create("https://stackoverflow.com/questions/333"));
        subscriptionRepository.save(chatId, link.id(), List.of());
        linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));

        Long now = Instant.now().getEpochSecond();
        stubStackOverflow("333", 200, """
            { "items": [ {
                "question_id": 333,
                "last_activity_date": %d,
                "comments": [ {
                    "owner": {"display_name": "commenter"},
                    "body": "Useful comment body",
                    "creation_date": %d
                } ]
            } ] }
            """.formatted(now, now));

        linkChecker.checkAllLinks();

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(botNotifier, timeout(5000).atLeastOnce()).notify(captor.capture());

        String msg = captor.getAllValues().stream()
                .filter(u -> u.getId().equals(link.id()))
                .findFirst()
                .orElseThrow()
                .getDescription();

        assertTrue(msg.contains("commenter") && msg.contains("Useful comment"));
    }

    @Test
    @DisplayName("Пакетная обработка: несколько StackOverflow ссылок за один HTTP запрос")
    void batchProcessing_MultipleLinksHandledTogether() {
        Long chatId = 200L;
        chatRepository.save(chatId);

        Link link1 = linkRepository.save(URI.create("https://stackoverflow.com/questions/301"));
        Link link2 = linkRepository.save(URI.create("https://stackoverflow.com/questions/302"));

        for (Link link : List.of(link1, link2)) {
            subscriptionRepository.save(chatId, link.id(), List.of());
            linkService.updateLastChecked(link.id(), Instant.now().minus(1, ChronoUnit.DAYS));
        }

        Long now = Instant.now().getEpochSecond();
        stubStackOverflow("301;302", 200, """
            { "items": [
                { "question_id": 301, "last_activity_date": %1$d, "answers": [ { "owner": {"display_name": "u1"}, "creation_date": %1$d } ] },
                { "question_id": 302, "last_activity_date": %1$d, "answers": [ { "owner": {"display_name": "u2"}, "creation_date": %1$d } ] }
            ] }
            """.formatted(now));

        linkChecker.checkAllLinks();

        verify(botNotifier, timeout(5000).atLeastOnce())
                .notify(argThat(u -> u.getId().equals(link1.id())));
        verify(botNotifier, timeout(5000).atLeastOnce())
                .notify(argThat(u -> u.getId().equals(link2.id())));
    }

    @Test
    @DisplayName("Resilience: падение одного API не блокирует проверку ссылок из другого")
    void resilience_OneApiFailureDoesNotStopWholeProcess() {
        Long chatId = 400L;
        chatRepository.save(chatId);

        Link ghLink = linkRepository.save(URI.create("https://github.com/user/error-repo"));
        Link soLink = linkRepository.save(URI.create("https://stackoverflow.com/questions/505"));

        for (Link l : List.of(ghLink, soLink)) {
            subscriptionRepository.save(chatId, l.id(), List.of());
            linkService.updateLastChecked(l.id(), Instant.now().minus(1, ChronoUnit.DAYS));
        }

        stubGitHub("user", "error-repo", 503, "Service Unavailable");

        Long now = Instant.now().getEpochSecond();
        stubStackOverflow("505", 200, """
            { "items": [ {
                "question_id": 505, "last_activity_date": %d, "answers": [{"owner":{"display_name":"so_pro"}, "creation_date": %d}]
            } ] }
            """.formatted(now, now));

        linkChecker.checkAllLinks();

        verify(botNotifier, timeout(5000).atLeastOnce())
                .notify(argThat(u -> u.getId().equals(soLink.id())));
    }
}
