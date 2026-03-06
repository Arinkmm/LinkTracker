package backend.academy.linktracker.scrapper.service.grpc;

import backend.academy.linktracker.grpc.*;
import backend.academy.linktracker.grpc.AddLinkRequest;
import backend.academy.linktracker.grpc.LinkResponse;
import backend.academy.linktracker.grpc.RemoveLinkRequest;
import backend.academy.linktracker.scrapper.exception.*;
import backend.academy.linktracker.scrapper.service.mapper.GrpcMapper;
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
    private final UserService service;
    private final GrpcMapper mapper;

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
            backend.academy.linktracker.scrapper.dto.AddLinkRequest dtoReq = mapper.grpcToDto(request);
            backend.academy.linktracker.scrapper.dto.LinkResponse dtoRes = service.addLink(request.getTgChatId(), dtoReq);
            LinkResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (LinkAlreadyTrackedException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void removeLink(RemoveLinkRequest request,
                           StreamObserver<LinkResponse> responseObserver) {
        try {
            backend.academy.linktracker.scrapper.dto.RemoveLinkRequest dtoReq = mapper.grpcToDto(request);
            backend.academy.linktracker.scrapper.dto.LinkResponse dtoRes = service.removeLink(request.getTgChatId(), dtoReq);
            LinkResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getLinks(GetLinksRequest request,
                         StreamObserver<ListLinksResponse> responseObserver) {
        try {
            backend.academy.linktracker.scrapper.dto.ListLinksResponse dtoRes = service.getLinks(request.getTgChatId());
            ListLinksResponse grpcRes = mapper.dtoToGrpc(dtoRes);
            responseObserver.onNext(grpcRes);
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
