package backend.academy.linktracker.scrapper.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.RedisClusterClient;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class ClientSideCacheManager implements CacheManager {
    private final RedisClusterClient clusterClient;
    private final Duration ttl;
    private final int l1Capacity;
    private final ObjectMapper objectMapper;
    private final Map<String, LettuceClientSideCache> caches = new ConcurrentHashMap<>();

    public ClientSideCacheManager(RedisClusterClient client, Duration ttl, int cap, ObjectMapper mapper) {
        this.clusterClient = client;
        this.ttl = ttl;
        this.l1Capacity = cap;
        this.objectMapper = mapper;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(
                name, n -> new LettuceClientSideCache(n, clusterClient, ttl, objectMapper, l1Capacity));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }

    @PreDestroy
    public void destroy() {
        caches.values().forEach(cache -> {
            try {
                cache.close();
            } catch (Exception ignored) {
            }
        });
    }
}
