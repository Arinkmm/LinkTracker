package backend.academy.linktracker.scrapper.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.configuration.CacheIntegrationEnvironment;
import backend.academy.linktracker.scrapper.dto.AddLinkRequest;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.dto.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.service.user.ChatService;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class CacheIntegrationTest extends CacheIntegrationEnvironment {
    @Autowired
    private LinkService linkService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RedisClusterClient redisClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ObjectMapper cacheObjectMapper;

    @MockitoSpyBean
    private SubscriptionRepository subscriptionRepository;

    private static final Long CHAT_ID = 777L;
    private static final String LINK_URL = "https://github.com/test/repo";
    private static final String CACHE_KEY = "links:" + CHAT_ID;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE subscriptions, links, chats RESTART IDENTITY CASCADE");
        cacheManager.getCache("links").clear();
        chatService.registerChat(CHAT_ID);
        reset(subscriptionRepository);
    }

    @Test
    @DisplayName("Повторный вызов getLinks не обращается к БД (кэш работает)")
    void shouldReturnCachedResultOnSecondCall() {
        linkService.getLinks(CHAT_ID);
        linkService.getLinks(CHAT_ID);

        verify(subscriptionRepository, times(1)).findSubscriptionByChatId(eq(CHAT_ID), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Данные — валидный JSON с полями links и size")
    void shouldStoreResponseAsJsonInValkey() throws Exception {
        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));
        linkService.getLinks(CHAT_ID);

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            String raw = conn.sync().get(CACHE_KEY);

            assertThat(raw).as("Ключ '%s' должен существовать", CACHE_KEY).isNotNull();

            ListLinksResponse stored = cacheObjectMapper.readValue(raw, ListLinksResponse.class);
            assertThat(stored.getSize()).isEqualTo(1);
            assertThat(stored.getLinks()).hasSize(1);
            assertThat(stored.getLinks().get(0).getUrl().toString()).isEqualTo(LINK_URL);
        }
    }

    @Test
    @DisplayName("Ключ имеет положительный TTL")
    void shouldHavePositiveTtlInValkey() {
        linkService.getLinks(CHAT_ID);

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            long ttl = conn.sync().ttl(CACHE_KEY);
            assertThat(ttl)
                    .as("TTL должен быть > 0, а не %d (ключ: %s)", ttl, CACHE_KEY)
                    .isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("После истечения TTL ключ физически исчезает")
    void shouldExpireKeyInValkeyAfterTtl() throws InterruptedException {
        linkService.getLinks(CHAT_ID);

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY)).isEqualTo(1L);
        }

        TimeUnit.SECONDS.sleep(3);

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY))
                    .as("Ключ должен удалиться после истечения TTL")
                    .isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("После истечения TTL следующий вызов снова идёт в БД")
    void shouldCallRepositoryAgainAfterTtlExpiration() throws InterruptedException {
        linkService.getLinks(CHAT_ID);
        linkService.getLinks(CHAT_ID);

        verify(subscriptionRepository, times(1)).findSubscriptionByChatId(eq(CHAT_ID), anyInt(), anyInt());

        TimeUnit.SECONDS.sleep(3);

        linkService.getLinks(CHAT_ID);

        verify(subscriptionRepository, times(2)).findSubscriptionByChatId(eq(CHAT_ID), anyInt(), anyInt());

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY))
                    .as("После повторного промаха кэш должен прогреться заново")
                    .isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("addLink инвалидирует ключ")
    void shouldEvictCacheOnLinkAdd() {
        linkService.getLinks(CHAT_ID);

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY)).isEqualTo(1L);
        }

        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY))
                    .as("Ключ должен удалиться после addLink")
                    .isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("removeLink инвалидирует ключ")
    void shouldEvictCacheOnLinkRemove() {
        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));
        linkService.getLinks(CHAT_ID);

        linkService.removeLink(CHAT_ID, removeLinkRequest(LINK_URL));

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY))
                    .as("Ключ должен удалиться после removeLink")
                    .isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("После инвалидации через addLink getLinks возвращает актуальные данные")
    void shouldReturnFreshDataAfterAddEviction() {
        assertThat(linkService.getLinks(CHAT_ID).getSize()).isZero();

        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));

        ListLinksResponse after = linkService.getLinks(CHAT_ID);
        assertThat(after.getSize()).isEqualTo(1);
        assertThat(after.getLinks().get(0).getUrl().toString()).isEqualTo(LINK_URL);
    }

    @Test
    @DisplayName("После инвалидации через removeLink getLinks возвращает пустой список")
    void shouldReturnEmptyListAfterRemoveEviction() {
        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));
        assertThat(linkService.getLinks(CHAT_ID).getSize()).isEqualTo(1);

        linkService.removeLink(CHAT_ID, removeLinkRequest(LINK_URL));

        assertThat(linkService.getLinks(CHAT_ID).getSize()).isZero();
    }

    @Test
    @DisplayName("Кэши разных пользователей изолированы")
    void shouldIsolateCachePerUser() {
        Long otherChatId = 888L;
        String otherCacheKey = "links:" + otherChatId;
        chatService.registerChat(otherChatId);

        linkService.addLink(CHAT_ID, addLinkRequest(LINK_URL));
        linkService.getLinks(CHAT_ID);
        linkService.getLinks(otherChatId);

        linkService.removeLink(CHAT_ID, removeLinkRequest(LINK_URL));

        try (StatefulRedisClusterConnection<String, String> conn = redisClient.connect()) {
            assertThat(conn.sync().exists(CACHE_KEY))
                    .as("Кэш пользователя 777 должен быть сброшен")
                    .isEqualTo(0L);
            assertThat(conn.sync().exists(otherCacheKey))
                    .as("Кэш пользователя 888 не должен быть затронут")
                    .isEqualTo(1L);
        }
    }

    private AddLinkRequest addLinkRequest(String url) {
        AddLinkRequest req = new AddLinkRequest();
        req.setLink(URI.create(url));
        return req;
    }

    private RemoveLinkRequest removeLinkRequest(String url) {
        RemoveLinkRequest req = new RemoveLinkRequest();
        req.setLink(URI.create(url));
        return req;
    }
}
