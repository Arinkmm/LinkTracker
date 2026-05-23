package backend.academy.linktracker.scrapper.resilience;

import static org.mockito.Mockito.mock;

import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.FallbackBotNotifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class FallbackBotNotifierTestConfiguration {
    static final BotNotifier PRIMARY_NOTIFIER = mock(BotNotifier.class);
    static final BotNotifier KAFKA_NOTIFIER = mock(BotNotifier.class);

    @Bean
    BotNotifier primaryNotifier() {
        return PRIMARY_NOTIFIER;
    }

    @Bean
    BotNotifier kafkaNotifier() {
        return KAFKA_NOTIFIER;
    }

    @Bean
    @Primary
    BotNotifier fallbackBotNotifier() {
        return new FallbackBotNotifier(primaryNotifier(), kafkaNotifier());
    }
}
