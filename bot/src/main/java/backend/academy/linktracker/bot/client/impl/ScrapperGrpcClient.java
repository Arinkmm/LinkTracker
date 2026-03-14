package backend.academy.linktracker.bot.client.impl;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exception.ApiException;
import backend.academy.linktracker.bot.service.mapper.GrpcMapper;
import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import io.grpc.StatusRuntimeException;
import java.net.URI;
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
    public LinkResponse addLink(Long id, URI url, List<String> tags, List<String> filters) {
        try {
            backend.academy.linktracker.grpc.LinkResponse r = blockingStub.addLink(AddLinkRequest.newBuilder()
                    .setTgChatId(id)
                    .setLink(url.toString())
                    .addAllTags(tags)
                    .addAllFilters(filters)
                    .build());
            return mapper.fromProto(r);
        } catch (StatusRuntimeException e) {
            throw new ApiException(toApiError(e));
        }
    }

    @Override
    public LinkResponse removeLink(Long id, URI url) {
        try {
            backend.academy.linktracker.grpc.LinkResponse r = blockingStub.removeLink(RemoveLinkRequest.newBuilder()
                    .setTgChatId(id)
                    .setLink(url.toString())
                    .build());
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

        ApiErrorResponse error = new ApiErrorResponse();
        error.setDescription(description);
        error.setCode(String.valueOf(e.getStatus().getCode().value()));
        error.setExceptionName(e.getClass().getSimpleName());
        error.setExceptionMessage(e.getMessage());
        error.setStacktrace(List.of());

        return error;
    }
}
