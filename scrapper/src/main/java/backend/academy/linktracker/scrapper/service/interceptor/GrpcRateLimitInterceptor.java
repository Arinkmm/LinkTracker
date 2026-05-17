package backend.academy.linktracker.scrapper.service.interceptor;

import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.GlobalServerInterceptor;

@GlobalServerInterceptor
@RequiredArgsConstructor
@Slf4j
public class GrpcRateLimitInterceptor implements ServerInterceptor {
    private final RateLimitProperties properties;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String client = resolveClient(call);
        RateLimiter rateLimiter = limiters.computeIfAbsent(client, this::createRateLimiter);

        if (rateLimiter.acquirePermission()) {
            return next.startCall(call, headers);
        }

        String method = call.getMethodDescriptor().getFullMethodName();
        log.atWarn()
                .addKeyValue("client", client)
                .addKeyValue("method", method)
                .log("gRPC rate limit exceeded, returning RESOURCE_EXHAUSTED");

        call.close(
                Status.RESOURCE_EXHAUSTED.withDescription("Rate limit exceeded. Please slow down"),
                new Metadata());
        return new ServerCall.Listener<>() {};
    }

    private RateLimiter createRateLimiter(String client) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getCapacity())
                .limitRefreshPeriod(properties.getRefillPeriod())
                .timeoutDuration(java.time.Duration.ZERO)
                .build();
        return RateLimiter.of("grpc-client-" + client, config);
    }

    private String resolveClient(ServerCall<?, ?> call) {
        SocketAddress remoteAddress = call.getAttributes().get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }
}
