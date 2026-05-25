package backend.academy.linktracker.bot.service.interceptor;

import backend.academy.linktracker.bot.properties.RateLimitProperties;
import backend.academy.linktracker.common.GrpcRateLimitInterceptorSupport;
import org.springframework.grpc.server.GlobalServerInterceptor;

@GlobalServerInterceptor
public class GrpcRateLimitInterceptor extends GrpcRateLimitInterceptorSupport {
    public GrpcRateLimitInterceptor(RateLimitProperties properties) {
        super(properties.getCapacity(), properties.getRefillPeriod(), properties.getMaxEntries());
    }
}
