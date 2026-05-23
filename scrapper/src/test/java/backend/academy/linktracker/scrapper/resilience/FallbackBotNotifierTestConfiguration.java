package backend.academy.linktracker.scrapper.resilience;

import static org.mockito.Mockito.mock;

import backend.academy.linktracker.scrapper.service.notifier.BotNotifier;
import backend.academy.linktracker.scrapper.service.notifier.impl.FallbackBotNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FallbackBotNotifierTestConfiguration {
    static final BotNotifier PRIMARY_NOTIFIER = mock(BotNotifier.class);
    static final BotNotifier KAFKA_NOTIFIER = mock(BotNotifier.class);

    @Bean
    BotNotifier testPrimaryNotifier() {
        return PRIMARY_NOTIFIER;
    }

    @Bean
    BotNotifier testKafkaNotifier() {
        return KAFKA_NOTIFIER;
    }

    @Bean
    BotNotifier testFallbackBotNotifier() {
        return new FallbackBotNotifier(testPrimaryNotifier(), testKafkaNotifier());
    }
}
