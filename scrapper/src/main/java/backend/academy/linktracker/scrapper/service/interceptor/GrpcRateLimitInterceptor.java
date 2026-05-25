package backend.academy.linktracker.scrapper.service.interceptor;

import backend.academy.linktracker.common.GrpcRateLimitInterceptorSupport;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import org.springframework.grpc.server.GlobalServerInterceptor;

@GlobalServerInterceptor
public class GrpcRateLimitInterceptor extends GrpcRateLimitInterceptorSupport {
    public GrpcRateLimitInterceptor(RateLimitProperties properties) {
        super(properties.getCapacity(), properties.getRefillPeriod(), properties.getMaxEntries());
    }
}
