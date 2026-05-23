package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.impl.BotGrpcClient;
import backend.academy.linktracker.scrapper.client.bot.impl.BotHttpClient;
import backend.academy.linktracker.scrapper.exception.BotClientExceptionFactory;
import backend.academy.linktracker.scrapper.properties.ClientTimeoutProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http")
    public RestClient botRestClient(
            @Value("${app.bot.url}") String url,
            ClientTimeoutProperties clientTimeoutProperties,
            BotClientExceptionFactory exceptionFactory) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(clientTimeoutProperties.getConnectTimeout()))
                .setConnectionRequestTimeout(Timeout.of(clientTimeoutProperties.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(clientTimeoutProperties.getReadTimeout()))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        requestFactory.setReadTimeout(clientTimeoutProperties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw exceptionFactory.fromHttpStatus(response.getStatusCode(), rawBody);
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotServiceGrpc.BotServiceBlockingStub botGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return BotServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http")
    public BotClient httpBotClient(RestClient botRestClient) {
        return new BotHttpClient(botRestClient);
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotClient grpcBotClient(
            BotServiceGrpc.BotServiceBlockingStub botGrpcStub,
            ClientTimeoutProperties clientTimeoutProperties,
            BotClientExceptionFactory exceptionFactory) {
        return new BotGrpcClient(botGrpcStub, clientTimeoutProperties, exceptionFactory);
    }
}
