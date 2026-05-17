package backend.academy.linktracker.scrapper.client.bot.impl;

import static backend.academy.linktracker.scrapper.exception.ApiExceptionMapper.fromGrpcException;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.grpc.Deadline;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BotGrpcClient implements BotClient {
    private final BotServiceGrpc.BotServiceBlockingStub stub;
    private final ResilienceProperties resilienceProperties;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            stub.withDeadline(Deadline.after(
                            resilienceProperties.getTimeout().getReadTimeout().toNanos(),
                            TimeUnit.NANOSECONDS))
                    .sendUpdate(backend.academy.linktracker.grpc.LinkUpdate.newBuilder()
                            .setId(linkUpdate.getId())
                            .setUrl(linkUpdate.getUrl().toString())
                            .setDescription(linkUpdate.getDescription())
                            .addAllTgChatIds(linkUpdate.getTgChatIds())
                            .build());
        } catch (StatusRuntimeException e) {
            log.atDebug()
                    .addKeyValue("id", linkUpdate.getId())
                    .log("gRPC Bot notification failed");
            throw fromGrpcException(e, "Bot");
        }
    }
}
