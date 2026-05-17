package backend.academy.linktracker.scrapper.resilience;

import backend.academy.linktracker.scrapper.exception.RateLimitExceededException;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import backend.academy.linktracker.scrapper.service.interceptor.RateLimitInterceptor;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {
    private static final int LIMIT = 3;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(LIMIT, Duration.ofMinutes(1));
        interceptor = new RateLimitInterceptor(properties);
    }

    @Test
    @DisplayName("Запросы в пределах лимита проходят")
    void whenRequestsWithinLimit_allPass() {
        MockHttpServletRequest request = requestFrom("1.2.3.4");

        for (int i = 0; i < LIMIT; i++) {
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), null)).isTrue();
        }
    }

    @Test
    @DisplayName("Запрос сверх лимита отклоняется")
    void whenRequestsExceedLimit_rejectsRequest() {
        MockHttpServletRequest request = requestFrom("5.6.7.8");

        for (int i = 0; i < LIMIT; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("5.6.7.8");
    }

    @Test
    @DisplayName("Лимит применяется независимо для каждого IP")
    void whenDifferentIps_limitsAreIndependent() {
        MockHttpServletRequest ip1 = requestFrom("10.0.0.1");
        MockHttpServletRequest ip2 = requestFrom("10.0.0.2");

        for (int i = 0; i < LIMIT; i++) {
            interceptor.preHandle(ip1, new MockHttpServletResponse(), null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(ip1, new MockHttpServletResponse(), null))
                .isInstanceOf(RateLimitExceededException.class);

        assertThat(interceptor.preHandle(ip2, new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    @DisplayName("Заголовок X-Forwarded-For используется как IP клиента")
    void whenForwardedHeader_usesRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
        request.setRemoteAddr("172.16.0.1");

        for (int i = 0; i < LIMIT; i++) {
            interceptor.preHandle(request, new MockHttpServletResponse(), null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("192.168.1.100");
    }

    private MockHttpServletRequest requestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }
}
