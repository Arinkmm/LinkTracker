package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ScrapperApiException;
import backend.academy.linktracker.grpc.ScrapperServiceGrpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;


@Configuration
public class HttpConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "http", matchIfMissing = true)
    public RestClient scrapperRestClient(
        @Value("${app.scrapper.url}") String url,
        ObjectMapper objectMapper) {
        return RestClient.builder()
            .baseUrl(url)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                ApiErrorResponse error = objectMapper.readValue(
                    response.getBody(), ApiErrorResponse.class);
                throw new ScrapperApiException(error);
            })
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public ScrapperServiceGrpc.ScrapperServiceBlockingStub scrapperGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return ScrapperServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
