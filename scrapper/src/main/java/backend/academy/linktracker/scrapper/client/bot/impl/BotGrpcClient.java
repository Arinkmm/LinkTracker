package backend.academy.linktracker.scrapper.client.bot.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exception.ApiException;
import io.grpc.StatusRuntimeException;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BotGrpcClient implements BotClient {
    private final BotServiceGrpc.BotServiceBlockingStub stub;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            stub.sendUpdate(backend.academy.linktracker.grpc.LinkUpdate.newBuilder()
                    .setId(linkUpdate.getId())
                    .setUrl(linkUpdate.getUrl().toString())
                    .setDescription(linkUpdate.getDescription())
                    .addAllTgChatIds(linkUpdate.getTgChatIds())
                    .build());
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    private ApiErrorResponse toApiError(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();

        ApiErrorResponse error = new ApiErrorResponse();
        error.setDescription(description);
        error.setCode(String.valueOf(e.getStatus().getCode().value()));
        error.setExceptionName(e.getClass().getSimpleName());
        error.setExceptionMessage(e.getMessage());
        error.setStacktrace(List.of());

        return error;
    }
}
