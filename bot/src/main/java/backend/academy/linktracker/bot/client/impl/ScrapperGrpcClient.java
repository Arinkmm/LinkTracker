package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.dto.ListLinksResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.service.mapper.GrpcMapper;
import backend.academy.linktracker.grpc.*;
import io.grpc.StatusRuntimeException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.client.type", havingValue = "grpc")
public class ScrapperGrpcClient implements ScrapperClient {
    private final ScrapperServiceGrpc.ScrapperServiceBlockingStub blockingStub;
    private final GrpcMapper mapper;

    @Override
    public void registerChat(Long id) {
        try {
            blockingStub.registerChat(RegisterChatRequest.newBuilder().setId(id).build());
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    @Override
    public void deleteChat(Long id) {
        try {
            blockingStub.deleteChat(DeleteChatRequest.newBuilder().setId(id).build());
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    @Override
    public LinkResponse addLink(Long id, String url, List<String> tags, List<String> filters) {
        try {
            backend.academy.linktracker.grpc.LinkResponse r = blockingStub.addLink(AddLinkRequest.newBuilder()
                    .setTgChatId(id)
                    .setLink(url)
                    .addAllTags(tags)
                    .addAllFilters(filters)
                    .build());
            return mapper.fromProto(r);
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    @Override
    public LinkResponse removeLink(Long id, String url) {
        try {
            backend.academy.linktracker.grpc.LinkResponse r = blockingStub.removeLink(
                    RemoveLinkRequest.newBuilder().setTgChatId(id).setLink(url).build());
            return mapper.fromProto(r);
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    @Override
    public ListLinksResponse getLinks(Long id) {
        try {
            backend.academy.linktracker.grpc.ListLinksResponse r = blockingStub.getLinks(
                    GetLinksRequest.newBuilder().setTgChatId(id).build());
            return mapper.fromProto(r);
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    private ApiErrorResponse toApiError(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();
        return new ApiErrorResponse(
                description,
                String.valueOf(e.getStatus().getCode().value()),
                e.getClass().getSimpleName(),
                e.getMessage(),
                List.of());
    }
}
