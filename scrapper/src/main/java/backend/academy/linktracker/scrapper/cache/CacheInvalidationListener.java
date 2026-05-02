package backend.academy.linktracker.scrapper.cache;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubListener;
import java.util.Map;

public class CacheInvalidationListener implements RedisClusterPubSubListener<String, String> {
    private final String cacheName;
    private final Map<String, String> l1Cache;

    public CacheInvalidationListener(String cacheName, Map<String, String> l1Cache) {
        this.cacheName = cacheName;
        this.l1Cache = l1Cache;
    }

    @Override
    public void message(RedisClusterNode node, String channel, String message) {
        if (message.startsWith(cacheName + ":")) {
            l1Cache.remove(message);
        }
    }

    @Override
    public void message(RedisClusterNode node, String pattern, String channel, String message) {
        message(node, channel, message);
    }

    @Override
    public void subscribed(RedisClusterNode node, String channel, long count) {}

    @Override
    public void psubscribed(RedisClusterNode node, String pattern, long count) {}

    @Override
    public void unsubscribed(RedisClusterNode node, String channel, long count) {}

    @Override
    public void punsubscribed(RedisClusterNode node, String pattern, long count) {}
}
