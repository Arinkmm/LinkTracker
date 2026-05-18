package backend.academy.linktracker.scrapper.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

@Slf4j
@RequiredArgsConstructor
public class ClientSideCacheManager extends AbstractCacheManager {
    private static final String[] KEYEVENT_PATTERNS = {"__keyevent@*__:del", "__keyevent@*__:expired"};

    private final StatefulRedisClusterConnection<String, String> connection;
    private final StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;

    private final Duration l1Ttl;
    private final Duration l2Ttl;
    private final int l1Capacity;
    private final ObjectMapper objectMapper;

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return List.of();
    }

    @Override
    protected Cache getMissingCache(String name) {
        return new LettuceClientSideCache(name, connection, pubSubConnection, l1Ttl, objectMapper, l1Capacity, l2Ttl);
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        pubSubConnection.sync().psubscribe(KEYEVENT_PATTERNS);
        log.atInfo().log("Subscribed to Redis keyevent patterns for cache invalidation");
    }

    @PreDestroy
    public void destroy() {
        try {
            pubSubConnection.sync().punsubscribe(KEYEVENT_PATTERNS);
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Failed to punsubscribe on destroy");
        }

        getCacheNames().forEach(name -> {
            Cache cache = getCache(name);
            if (cache instanceof LettuceClientSideCache lettuceCache) {
                try {
                    lettuceCache.close();
                } catch (Exception e) {
                    log.atError()
                            .addKeyValue("cacheName", name)
                            .addKeyValue("errorMessage", e.getMessage())
                            .setCause(e)
                            .log("Failed to close cache on destroy");
                }
            }
        });
    }
}
