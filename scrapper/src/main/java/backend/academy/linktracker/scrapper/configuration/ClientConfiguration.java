package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.github.GitHubClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ClientConfiguration {
    private final GithubProperties githubProperties;
    private final StackoverflowProperties stackoverflowProperties;

    @Bean
    public GitHubClient gitHubClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(githubProperties.getUrl())
                .defaultHeader("Authorization", "Bearer " + githubProperties.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(GitHubClient.class);
    }

    @Bean
    public StackOverflowClient stackOverflowClient() {
        RestClient restClient =
                RestClient.builder().baseUrl(stackoverflowProperties.getUrl()).build();

        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(StackOverflowClient.class);
    }
}
