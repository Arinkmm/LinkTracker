package backend.academy.linktracker.scrapper.configuration;

import static backend.academy.linktracker.scrapper.exception.ApiExceptionMapper.fromHttpResponse;

import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.client.bot.impl.BotGrpcClient;
import backend.academy.linktracker.scrapper.client.bot.impl.BotHttpClient;
import backend.academy.linktracker.scrapper.properties.ErrorProperties;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannelBuilder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ClientConfiguration {
    private final ErrorProperties errorProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http")
    public RestClient botRestClient(
            @Value("${app.bot.url}") String url,
            ObjectMapper objectMapper,
            ResilienceProperties resilienceProperties) {

        ResilienceProperties.Timeout t = resilienceProperties.getTimeout();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(t.getConnectTimeout()))
                .setConnectionRequestTimeout(Timeout.of(t.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(t.getReadTimeout()))
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().setDefaultRequestConfig(requestConfig).build());
        requestFactory.setReadTimeout(t.getReadTimeout());

        return RestClient.builder()
                .baseUrl(url)
                .requestFactory(requestFactory)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String rawBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw fromHttpResponse(
                            objectMapper,
                            response.getStatusCode(),
                            rawBody,
                            errorProperties.getInvalidResponse(),
                            "Bot");
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotServiceGrpc.BotServiceBlockingStub botGrpcStub(
            @Value("${app.grpc.host}") String host,
            @Value("${app.grpc.port}") int port) {
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
            ResilienceProperties resilienceProperties) {
        return new BotGrpcClient(botGrpcStub, resilienceProperties);
    }
}
