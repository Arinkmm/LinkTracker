package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ThreadProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadConfiguration {
    private ExecutorService executorService;

    @Bean
    public ExecutorService linkExecutorService(ThreadProperties threadProperties) {
        executorService = Executors.newFixedThreadPool(threadProperties.getExecutedThreads());
        return executorService;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
