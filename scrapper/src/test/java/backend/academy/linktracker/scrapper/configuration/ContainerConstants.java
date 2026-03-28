package backend.academy.linktracker.scrapper.configuration;

public final class ContainerConstants {
    public static final String POSTGRES_IMAGE = "postgres:17";
    public static final String DB_NAME = "scrapper";
    public static final String DB_USER = "user";
    public static final String DB_PASSWORD = "password";
    public static final String DB_DRIVER = "org.postgresql.Driver";
    public static final String DB_NETWORK_ALIAS = "postgres-db";
    public static final String LIQUIBASE_PATH = "classpath:migrations/master.xml";
    public static final int APP_PORT = 8081;
    public static final String APP_JAR = "target/scrapper-0.0.1.jar";
    public static final String APP_IMAGE = "localhost/link-tracker-scrapper-e2e:latest";

    private ContainerConstants() {}
}
