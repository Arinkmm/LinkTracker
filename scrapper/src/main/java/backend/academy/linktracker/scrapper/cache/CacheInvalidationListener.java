package backend.academy.linktracker.scrapper.cache;

import com.github.benmanes.caffeine.cache.Cache;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CacheInvalidationListener extends RedisClusterPubSubAdapter<String, String> {
    private final String cacheName;
    private final Cache<Object, Object> l1Cache;

    @Override
    public void message(RedisClusterNode node, String channel, String message) {
        String prefix = cacheName + ":";
        if (message.startsWith(prefix)) {
            String localKey = message.substring(prefix.length());
            l1Cache.invalidate(localKey);
        }
    }

    @Override
    public void message(RedisClusterNode node, String pattern, String channel, String message) {
        message(node, channel, message);
    }
}
