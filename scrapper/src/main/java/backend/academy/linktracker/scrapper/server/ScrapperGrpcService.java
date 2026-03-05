package backend.academy.linktracker.scrapper.server;

import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.grpc.AddLinkRequest;
import backend.academy.linktracker.grpc.LinkResponse;
import backend.academy.linktracker.grpc.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.service.ScrapperService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ScrapperGrpcService extends ScrapperServiceGrpc.ScrapperServiceImplBase {
    private final ScrapperService service;

    @Override
    public void registerChat(RegisterChatRequest request,
                             StreamObserver<EmptyResponse> responseObserver) {
        try {
            service.registerChat(request.getId());
            responseObserver.onNext(EmptyResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ChatAlreadyExistsException e) {
            log.warn("Chat already exists: {}", request.getId());
            responseObserver.onError(
                Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteChat(DeleteChatRequest request,
                           StreamObserver<EmptyResponse> responseObserver) {
        try {
            service.deleteChat(request.getId());
            responseObserver.onNext(EmptyResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            log.warn("Chat not found: {}", request.getId());
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void addLink(AddLinkRequest request,
                        StreamObserver<LinkResponse> responseObserver) {
        try {
            backend.academy.linktracker.scrapper.client.bot.dto.AddLinkRequest dto = new backend.academy.linktracker.scrapper.client.bot.dto.AddLinkRequest(
                request.getLink(), request.getTagsList(), request.getFiltersList());
            backend.academy.linktracker.scrapper.client.bot.dto.LinkResponse result =
                service.addLink(request.getTgChatId(), dto);
            responseObserver.onNext(toProto(result));
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (LinkAlreadyTrackedException e) {
            responseObserver.onError(
                Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void removeLink(RemoveLinkRequest request,
                           StreamObserver<LinkResponse> responseObserver) {
        try {
            backend.academy.linktracker.scrapper.client.bot.dto.RemoveLinkRequest dto = new backend.academy.linktracker.scrapper.client.bot.dto.RemoveLinkRequest(request.getLink());
            backend.academy.linktracker.scrapper.client.bot.dto.LinkResponse result =
                service.removeLink(request.getTgChatId(), dto);
            responseObserver.onNext(toProto(result));
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getLinks(GetLinksRequest request,
                         StreamObserver<ListLinksResponse> responseObserver) {
        try {
            backend.academy.linktracker.scrapper.client.bot.dto.ListLinksResponse result = service.getLinks(request.getTgChatId());
            responseObserver.onNext(ListLinksResponse.newBuilder()
                .addAllLinks(result.links().stream()
                    .map(this::toProto)
                    .collect(Collectors.toList()))
                .setSize(result.size())
                .build());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private LinkResponse toProto(
        backend.academy.linktracker.scrapper.client.bot.dto.LinkResponse dto) {
        return LinkResponse.newBuilder()
            .setId(dto.id())
            .setUrl(dto.url())
            .addAllTags(dto.tags())
            .addAllFilters(dto.filters())
            .build();
    }
}
