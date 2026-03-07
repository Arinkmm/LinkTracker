package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    public RestClient scrapperRestClient(@Value("${app.bot.url}") String url, ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    ApiErrorResponse error = objectMapper.readValue(response.getBody(), ApiErrorResponse.class);
                    throw new ApiException(error);
                })
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
    public BotServiceGrpc.BotServiceBlockingStub scrapperGrpcStub(
            @Value("${app.grpc.host}") String host, @Value("${app.grpc.port}") int port) {
        return BotServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }
}
