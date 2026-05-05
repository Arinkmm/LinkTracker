package backend.academy.linktracker.bot.service.grpc;

import backend.academy.linktracker.bot.service.bot.TelegramSender;
import backend.academy.linktracker.grpc.BotServiceGrpc;
import backend.academy.linktracker.grpc.EmptyResponse;
import backend.academy.linktracker.grpc.LinkUpdate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@ConditionalOnProperty(name = "app.updates.type", havingValue = "grpc")
@RequiredArgsConstructor
@Slf4j
public class BotGrpcService extends BotServiceGrpc.BotServiceImplBase {
    private final TelegramSender telegramSender;

    @Override
    public void sendUpdate(LinkUpdate req, StreamObserver<EmptyResponse> resp) {
        try {
            req.getTgChatIdsList().forEach(id -> telegramSender.sendMessage(id, req.getDescription()));
            resp.onNext(EmptyResponse.getDefaultInstance());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
