package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.cache.ClientSideCacheManager;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.protocol.ProtocolVersion;
import java.time.Duration;
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
public class RedisConfiguration {

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
            RedisClusterClient clusterClient,
            @Value("${spring.cache.redis.time-to-live:10m}") Duration ttl,
            ObjectMapper cacheObjectMapper) {
        return new ClientSideCacheManager(clusterClient, ttl, properties.getL1Capacity(), cacheObjectMapper);
    }
}
