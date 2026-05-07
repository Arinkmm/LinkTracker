package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.cache.ClientSideCacheManager;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.protocol.ProtocolVersion;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
@RequiredArgsConstructor
public class CacheConfiguration {
    private final CacheProperties properties;

    @Bean(destroyMethod = "shutdown")
    public RedisClusterClient redisClusterClient(@Value("${spring.data.redis.cluster.nodes}") List<String> nodes) {
        List<RedisURI> uris = nodes.stream()
                .map(node -> {
                    String[] parts = node.split(":");
                    return RedisURI.create(parts[0], Integer.parseInt(parts[1]));
                })
                .toList();

        RedisClusterClient client = RedisClusterClient.create(uris);
        client.setOptions(ClusterClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build());
        return client;
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisClusterConnection<String, String> redisConnection(RedisClusterClient client) {
        return client.connect();
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisClusterPubSubConnection<String, String> pubSubConnection(RedisClusterClient client) {
        return client.connectPubSub();
    }

    @Bean
    public ObjectMapper cacheObjectMapper() {
        return new ObjectMapper()
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);
    }

    @Bean
    public CacheManager cacheManager(
            StatefulRedisClusterConnection<String, String> connection,
            StatefulRedisClusterPubSubConnection<String, String> pubSubConnection,
            ObjectMapper cacheObjectMapper) {
        return new ClientSideCacheManager(
                connection,
                pubSubConnection,
                properties.getL1Ttl(),
                properties.getL2Ttl(),
                properties.getL1Capacity(),
                cacheObjectMapper);
    }
}
