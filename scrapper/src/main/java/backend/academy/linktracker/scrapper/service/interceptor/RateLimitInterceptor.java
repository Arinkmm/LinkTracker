package backend.academy.linktracker.scrapper.service.interceptor;

import backend.academy.linktracker.scrapper.exception.RateLimitExceededException;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitProperties properties;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = resolveClientIp(request);
        RateLimiter rateLimiter = limiters.computeIfAbsent(ip, this::createRateLimiter);

        if (rateLimiter.acquirePermission()) {
            return true;
        }

        log.atWarn()
                .addKeyValue("ip", ip)
                .addKeyValue("path", request.getRequestURI())
                .log("Rate limit exceeded, returning HTTP 429");

        throw new RateLimitExceededException(ip, request.getRequestURI());
    }

    private RateLimiter createRateLimiter(String ip) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getCapacity())
                .limitRefreshPeriod(properties.getRefillPeriod())
                .timeoutDuration(java.time.Duration.ZERO)
                .build();
        return RateLimiter.of("http-ip-" + ip, config);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
