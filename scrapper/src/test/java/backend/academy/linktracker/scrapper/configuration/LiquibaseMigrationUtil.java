package backend.academy.linktracker.scrapper.configuration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

public final class LiquibaseMigrationUtil {
    public static void runMigrations(PostgreSQLContainer<?> postgres, String changeLogPath) {
        try {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(postgres.getJdbcUrl());
            dataSource.setUsername(postgres.getUsername());
            dataSource.setPassword(postgres.getPassword());

            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(changeLogPath);
            liquibase.setResourceLoader(new DefaultResourceLoader());
            liquibase.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Liquibase migrations from " + changeLogPath, e);
        }
    }
}
