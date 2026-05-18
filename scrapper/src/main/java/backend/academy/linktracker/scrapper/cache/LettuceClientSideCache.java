package backend.academy.linktracker.scrapper.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;

@Slf4j
public class LettuceClientSideCache extends CaffeineCache implements Closeable {
    private final StatefulRedisClusterConnection<String, String> connection;
    private final StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;
    private final long ttlSeconds;
    private final ObjectMapper objectMapper;

    private final CacheInvalidationListener invalidationListener;

    public LettuceClientSideCache(
            String name,
            StatefulRedisClusterConnection<String, String> connection,
            StatefulRedisClusterPubSubConnection<String, String> pubSubConnection,
            Duration l1Ttl,
            ObjectMapper mapper,
            int cap,
            Duration l2Ttl) {
        super(
                name,
                Caffeine.newBuilder().maximumSize(cap).expireAfterWrite(l1Ttl).build(),
                false);

        this.ttlSeconds = l2Ttl.toSeconds();
        this.objectMapper = mapper;
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;

        this.invalidationListener = new CacheInvalidationListener(name, getNativeCache());
        this.pubSubConnection.addListener(invalidationListener);
    }

    @Override
    protected Object lookup(Object key) {
        Object fromL1 = super.lookup(key);
        if (fromL1 != null) {
            return fromL1;
        }

        String fullKey = getName() + ":" + key;
        try {
            String fromRedis = connection.sync().get(fullKey);
            if (fromRedis != null) {
                Object value = deserialize(fromRedis);
                if (value != null) {
                    getNativeCache().put(key, value);
                }
                return value;
            }
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("cacheName", getName())
                    .addKeyValue("key", key)
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Redis error during lookup");
        }
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        super.put(key, value);
        try {
            String json = objectMapper.writeValueAsString(value);
            connection.sync().setex(getName() + ":" + key, ttlSeconds, json);
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("cacheName", getName())
                    .addKeyValue("key", key)
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Failed to sync put with Redis");
        }
    }

    @Override
    public void evict(Object key) {
        super.evict(key);
        try {
            connection.sync().del(getName() + ":" + key);
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("cacheName", getName())
                    .addKeyValue("key", key)
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Failed to evict key from Redis");
        }
    }

    @Override
    public void clear() {
        super.clear();
        try {
            List<String> keys = connection.sync().keys(getName() + ":*");
            if (!keys.isEmpty()) {
                connection.sync().del(keys.toArray(new String[0]));
            }
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("cacheName", getName())
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Failed to clear L2 cache");
        }
    }

    @Override
    public void close() {
        pubSubConnection.removeListener(invalidationListener);
    }

    private Object deserialize(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            log.atError()
                    .addKeyValue("errorMessage", e.getMessage())
                    .setCause(e)
                    .log("Deserialization error");
            return null;
        }
    }
}
