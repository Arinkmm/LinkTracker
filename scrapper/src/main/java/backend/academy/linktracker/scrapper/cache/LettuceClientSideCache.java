package backend.academy.linktracker.scrapper.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

public class LettuceClientSideCache implements Cache, Closeable {
    private final String name;
    private final StatefulRedisClusterConnection<String, String> connection;
    private final StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;
    private final long ttlSeconds;
    private final Map<String, String> l1Cache;
    private final ObjectMapper objectMapper;

    public LettuceClientSideCache(String name, RedisClusterClient client, Duration ttl, ObjectMapper mapper, int cap) {
        this.name = name;
        this.ttlSeconds = ttl.toSeconds();
        this.objectMapper = mapper;
        this.connection = client.connect();

        this.l1Cache = Collections.synchronizedMap(new LinkedHashMap<>(cap, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > cap;
            }
        });

        this.pubSubConnection = client.connectPubSub();
        this.pubSubConnection.addListener(new CacheInvalidationListener(name, l1Cache));
        this.pubSubConnection.sync().psubscribe("__keyevent@*__:del", "__keyevent@*__:expired");
    }

    @Override
    public ValueWrapper get(Object key) {
        String fullKey = name + ":" + key;

        String cached = l1Cache.get(fullKey);
        if (cached != null) {
            return deserialize(cached);
        }

        String fromRedis = connection.sync().get(fullKey);

        if (fromRedis != null) {
            l1Cache.put(fullKey, fromRedis);
            return deserialize(fromRedis);
        }
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            String fullKey = name + ":" + key;
            connection.sync().setex(fullKey, ttlSeconds, json);
            l1Cache.put(fullKey, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void evict(Object key) {
        String fullKey = name + ":" + key;
        connection.sync().del(fullKey);
        l1Cache.remove(fullKey);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return connection;
    }

    @Override
    public void clear() {
        l1Cache.clear();
    }

    private ValueWrapper deserialize(String json) {
        try {
            return new SimpleValueWrapper(objectMapper.readValue(json, Object.class));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper w = get(key);
        return w != null ? (T) w.get() : null;
    }

    @Override
    public <T> T get(Object key, Callable<T> loader) {
        ValueWrapper w = get(key);
        if (w != null) return (T) w.get();
        try {
            T val = loader.call();
            put(key, val);
            return val;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, loader, e);
        }
    }

    @Override
    public void close() {
        try {
            pubSubConnection.close();
            connection.close();
        } catch (Exception ignored) {
        }
    }
}
