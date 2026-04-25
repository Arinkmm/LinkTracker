package backend.academy.linktracker.scrapper.configuration;

public final class ContainerConstants {
    public static final String POSTGRES_IMAGE = "postgres:17";
    public static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.0";
    public static final String SCHEMA_REGISTRY_IMAGE = "confluentinc/cp-schema-registry:7.6.0";
    public static final String WIREMOCK_IMAGE = "wiremock/wiremock:3.13.1";

    public static final String DB_NAME = "scrapper";
    public static final String DB_USER = "user";
    public static final String DB_PASSWORD = "password";
    public static final String DB_DRIVER = "org.postgresql.Driver";
    public static final String DB_NETWORK_ALIAS = "postgres-db";
    public static final String LIQUIBASE_PATH = "classpath:migrations/master.xml";

    public static final int SCRAPPER_PORT = 8081;
    public static final int BOT_PORT = 8080;
    public static final int WIREMOCK_PORT = 8080;
    public static final int SCHEMA_REGISTRY_PORT = 8081;

    public static final String KAFKA_ALIAS = "kafka";
    public static final String SCHEMA_REGISTRY_ALIAS = "schema-registry";
    public static final String WIREMOCK_TG_ALIAS = "wiremock-tg";
    public static final String WIREMOCK_EXT_ALIAS = "wiremock-ext";
    public static final String SCRAPPER_ALIAS = "scrapper";

    public static final String TOPIC = "link-updates";

    public static final String SCRAPPER_JAR = "target/scrapper-0.0.1.jar";
    public static final String BOT_JAR = "../bot/target/bot-0.0.1.jar";

    private ContainerConstants() {}
}
