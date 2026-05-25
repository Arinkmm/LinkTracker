package backend.academy.linktracker.scrapper.service.interceptor;

import backend.academy.linktracker.common.HttpRateLimitInterceptorSupport;
import backend.academy.linktracker.scrapper.exception.RateLimitExceededException;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import org.springframework.stereotype.Component;

@Component
public class RateLimitInterceptor extends HttpRateLimitInterceptorSupport {
    public RateLimitInterceptor(RateLimitProperties properties) {
        super(properties.getCapacity(), properties.getRefillPeriod(), properties.getMaxEntries());
    }

    @Override
    protected RuntimeException rateLimitExceeded(String client, String path) {
        return new RateLimitExceededException(client, path);
    }
}
