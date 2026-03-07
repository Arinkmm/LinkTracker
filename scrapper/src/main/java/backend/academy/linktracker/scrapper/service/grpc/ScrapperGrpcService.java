package backend.academy.linktracker.scrapper.service.grpc;

import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.grpc.AddLinkRequest;
import backend.academy.linktracker.grpc.LinkResponse;
import backend.academy.linktracker.grpc.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.service.mapper.GrpcMapper;
import backend.academy.linktracker.scrapper.service.user.LinkService;
import backend.academy.linktracker.scrapper.service.user.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class ScrapperGrpcService extends ScrapperServiceGrpc.ScrapperServiceImplBase {
    private final UserService userService;
    private final LinkService linkService;
    private final GrpcMapper mapper;

    @Override
    public void registerChat(RegisterChatRequest request, StreamObserver<EmptyResponse> responseObserver) {
        Long id = request.getId();

        log.atDebug().addKeyValue("id", id).log("gRPC registerChat");

        try {
            userService.registerChat(id);
            responseObserver.onNext(EmptyResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ChatAlreadyExistsException e) {
            log.atWarn().addKeyValue("id", id).log("gRPC registerChat: chat already exists");

            responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void deleteChat(DeleteChatRequest request, StreamObserver<EmptyResponse> responseObserver) {
        Long id = request.getId();

        log.atDebug().addKeyValue("id", id).log("gRPC deleteChat");

        try {
            userService.deleteChat(id);
            responseObserver.onNext(EmptyResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            log.atWarn().addKeyValue("id", id).log("gRPC deleteChat: chat not found");

            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void addLink(AddLinkRequest request, StreamObserver<LinkResponse> responseObserver) {
        Long id = request.getTgChatId();

        log.atDebug()
                .addKeyValue("id", id)
                .addKeyValue("url", request.getLink())
                .addKeyValue("tags_count", request.getTagsCount())
                .log("gRPC addLink");

        try {
            backend.academy.linktracker.scrapper.dto.AddLinkRequest dtoReq = mapper.grpcToDto(request);
            backend.academy.linktracker.scrapper.dto.LinkResponse dtoRes = linkService.addLink(id, dtoReq);
            LinkResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            log.atWarn().addKeyValue("id", id).log("gRPC addLink: chat not found");

            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (LinkAlreadyTrackedException e) {
            log.atWarn()
                    .addKeyValue("id", id)
                    .addKeyValue("url", request.getLink())
                    .log("gRPC addLink: link already tracked");

            responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void removeLink(RemoveLinkRequest request, StreamObserver<LinkResponse> responseObserver) {
        Long id = request.getTgChatId();

        log.atDebug()
                .addKeyValue("id", id)
                .addKeyValue("url", request.getLink())
                .log("gRPC removeLink");

        try {
            backend.academy.linktracker.scrapper.dto.RemoveLinkRequest dtoReq = mapper.grpcToDto(request);
            backend.academy.linktracker.scrapper.dto.LinkResponse dtoRes = linkService.removeLink(id, dtoReq);
            LinkResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            log.atWarn().addKeyValue("id", id).log("gRPC removeLink: chat not found");

            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getLinks(GetLinksRequest request, StreamObserver<ListLinksResponse> responseObserver) {
        Long id = request.getTgChatId();

        log.atDebug().addKeyValue("id", id).log("gRPC getLinks");

        try {
            backend.academy.linktracker.scrapper.dto.ListLinksResponse dtoRes = linkService.getLinks(id);
            ListLinksResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            log.atWarn().addKeyValue("id", id).log("gRPC getLinks: chat not found");

            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
