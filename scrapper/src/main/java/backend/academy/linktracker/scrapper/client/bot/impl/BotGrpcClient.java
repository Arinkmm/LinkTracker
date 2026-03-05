package backend.academy.linktracker.scrapper.client.bot.impl;

import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.exception.ScrapperApiException;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
@RequiredArgsConstructor
public class BotGrpcClient implements BotClient {
    private final BotServiceGrpc.BotServiceBlockingStub stub;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            stub.sendUpdate(backend.academy.linktracker.grpc.LinkUpdate.newBuilder()
                .setId(linkUpdate.id())
                .setUrl(linkUpdate.url())
                .setDescription(linkUpdate.description())
                .addAllTgChatIds(linkUpdate.tgChatIds())
                .build());
        } catch (StatusRuntimeException e) {
            throw new ScrapperApiException(toApiError(e));
        }
    }

    private ApiErrorResponse toApiError(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();
        return new ApiErrorResponse(
            description,
            String.valueOf(e.getStatus().getCode().value()),
            e.getClass().getSimpleName(),
            e.getMessage(),
            List.of()
        );
    }
}
