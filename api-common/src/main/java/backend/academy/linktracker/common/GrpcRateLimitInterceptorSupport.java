package backend.academy.linktracker.common;

import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcRateLimitInterceptorSupport implements ServerInterceptor {
    private static final Metadata.Key<String> TG_CHAT_ID =
            Metadata.Key.of("tg-chat-id", Metadata.ASCII_STRING_MARSHALLER);

    private final RateLimiters limiters;

    protected GrpcRateLimitInterceptorSupport(int capacity, Duration refillPeriod, int maxEntries) {
        this.limiters = new RateLimiters(capacity, refillPeriod, maxEntries, "grpc-client");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String client = resolveClient(call, headers);

        if (limiters.acquire(client)) {
            return next.startCall(call, headers);
        }

        String method = call.getMethodDescriptor().getFullMethodName();
        log.atWarn()
                .addKeyValue("client", client)
                .addKeyValue("method", method)
                .log("gRPC rate limit exceeded, returning RESOURCE_EXHAUSTED");

        call.close(Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded. Please slow down"), new Metadata());
        return new ServerCall.Listener<>() {};
    }

    private String resolveClient(ServerCall<?, ?> call, Metadata headers) {
        String chatId = headers.get(TG_CHAT_ID);
        if (chatId != null && !chatId.isBlank()) {
            return "tg-chat-" + chatId;
        }

        SocketAddress remoteAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }
}
