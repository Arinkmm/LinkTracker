package backend.academy.linktracker.scrapper.client.bot.impl;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.scrapper.client.bot.BotClient;
import backend.academy.linktracker.scrapper.exception.BotClientExceptionFactory;
import backend.academy.linktracker.scrapper.properties.ClientTimeoutProperties;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BotGrpcClient implements BotClient {
    private static final Metadata.Key<String> TG_CHAT_ID =
            Metadata.Key.of("tg-chat-id", Metadata.ASCII_STRING_MARSHALLER);

    private final BotServiceGrpc.BotServiceBlockingStub stub;
    private final ClientTimeoutProperties clientTimeoutProperties;
    private final BotClientExceptionFactory exceptionFactory;

    @Override
    public void sendUpdate(LinkUpdate linkUpdate) {
        try {
            stubWithDeadline(linkUpdate)
                    .sendUpdate(backend.academy.linktracker.grpc.LinkUpdate.newBuilder()
                            .setId(linkUpdate.getId())
                            .setUrl(linkUpdate.getUrl().toString())
                            .setDescription(linkUpdate.getDescription())
                            .addAllTgChatIds(linkUpdate.getTgChatIds())
                            .build());
        } catch (StatusRuntimeException e) {
            log.atDebug()
                    .setCause(e)
                    .addKeyValue("id", linkUpdate.getId())
                    .addKeyValue("grpcStatus", e.getStatus().getCode())
                    .log("gRPC Bot notification failed");
            throw exceptionFactory.fromGrpcStatus(e);
        }
    }

    private BotServiceGrpc.BotServiceBlockingStub stubWithDeadline(LinkUpdate linkUpdate) {
        Metadata metadata = new Metadata();
        if (linkUpdate.getTgChatIds() != null && !linkUpdate.getTgChatIds().isEmpty()) {
            metadata.put(TG_CHAT_ID, String.valueOf(linkUpdate.getTgChatIds().getFirst()));
        }
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadline(
                        Deadline.after(clientTimeoutProperties.getReadTimeout().toNanos(), TimeUnit.NANOSECONDS));
    }
}
