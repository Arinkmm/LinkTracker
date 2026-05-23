package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperTransportClient;
import backend.academy.linktracker.bot.exception.ScrapperClientExceptionFactory;
import backend.academy.linktracker.bot.properties.ClientTimeoutProperties;
import backend.academy.linktracker.bot.service.mapper.GrpcMapper;
import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scrapper-client.type", havingValue = "grpc")
@Slf4j
public class ScrapperGrpcClient implements ScrapperTransportClient {
    private static final Metadata.Key<String> TG_CHAT_ID =
            Metadata.Key.of("tg-chat-id", Metadata.ASCII_STRING_MARSHALLER);

    private final ScrapperServiceGrpc.ScrapperServiceBlockingStub blockingStub;
    private final GrpcMapper mapper;
    private final ClientTimeoutProperties clientTimeoutProperties;
    private final ScrapperClientExceptionFactory exceptionFactory;

    @Override
    public void registerChat(Long id) {
        grpcCall("registerChat", () -> stubWithDeadline(id)
                .registerChat(RegisterChatRequest.newBuilder().setId(id).build()));
    }

    @Override
    public void deleteChat(Long id) {
        grpcCall("deleteChat", () -> stubWithDeadline(id)
                .deleteChat(DeleteChatRequest.newBuilder().setId(id).build()));
    }

    @Override
    public LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters) {
        backend.academy.linktracker.grpc.LinkResponse response = grpcCall("addLink", () -> stubWithDeadline(id)
                .addLink(AddLinkRequest.newBuilder()
                        .setTgChatId(id)
                        .setLink(url.toString())
                        .addAllTags(tags)
                        .addAllFilters(filters)
                        .build()));
        return mapper.fromProto(response);
    }

    @Override
    public LinkResponse removeLink(Long id, URI url) {
        backend.academy.linktracker.grpc.LinkResponse response = grpcCall("removeLink", () -> stubWithDeadline(id)
                .removeLink(RemoveLinkRequest.newBuilder()
                        .setTgChatId(id)
                        .setLink(url.toString())
                        .build()));
        return mapper.fromProto(response);
    }

    @Override
    public ListLinksResponse getLinks(Long id) {
        backend.academy.linktracker.grpc.ListLinksResponse response = grpcCall("getLinks", () -> stubWithDeadline(id)
                .getLinks(GetLinksRequest.newBuilder().setTgChatId(id).build()));
        return mapper.fromProto(response);
    }

    private ScrapperServiceGrpc.ScrapperServiceBlockingStub stubWithDeadline(Long tgChatId) {
        Metadata metadata = new Metadata();
        metadata.put(TG_CHAT_ID, String.valueOf(tgChatId));
        return blockingStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadline(
                        Deadline.after(clientTimeoutProperties.getReadTimeout().toNanos(), TimeUnit.NANOSECONDS));
    }

    private <T> T grpcCall(String operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (StatusRuntimeException e) {
            log.atDebug()
                    .setCause(e)
                    .addKeyValue("operation", operation)
                    .addKeyValue("grpcStatus", e.getStatus().getCode())
                    .log("gRPC Scrapper call failed");
            throw exceptionFactory.fromGrpcStatus(e);
        }
    }
}
