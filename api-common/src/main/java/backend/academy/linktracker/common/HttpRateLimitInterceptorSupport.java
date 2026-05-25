package backend.academy.linktracker.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public abstract class HttpRateLimitInterceptorSupport implements HandlerInterceptor {
    private final RateLimiters limiters;

    protected HttpRateLimitInterceptorSupport(int capacity, Duration refillPeriod, int maxEntries) {
        this.limiters = new RateLimiters(capacity, refillPeriod, maxEntries, "http-ip");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = request.getRemoteAddr();

        if (limiters.acquire(ip)) {
            return true;
        }

        log.atWarn()
                .addKeyValue("ip", ip)
                .addKeyValue("path", request.getRequestURI())
                .log("Rate limit exceeded, returning HTTP 429");

        throw rateLimitExceeded(ip, request.getRequestURI());
    }

    protected abstract RuntimeException rateLimitExceeded(String client, String path);
}
