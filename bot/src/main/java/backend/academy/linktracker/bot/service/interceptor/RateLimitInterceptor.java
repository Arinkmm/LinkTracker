package backend.academy.linktracker.bot.service.interceptor;

import backend.academy.linktracker.bot.exception.RateLimitExceededException;
import backend.academy.linktracker.bot.properties.RateLimitProperties;
import backend.academy.linktracker.common.HttpRateLimitInterceptorSupport;
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
