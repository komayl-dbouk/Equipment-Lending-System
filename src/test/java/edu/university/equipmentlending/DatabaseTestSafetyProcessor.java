package edu.university.equipmentlending;

import java.net.URI;
import java.util.Properties;
import org.postgresql.Driver;
import org.postgresql.PGProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

public final class DatabaseTestSafetyProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("database-test"))) return;
        String target = databaseName(environment.getProperty("spring.datasource.url"));
        String appUrl = environment.getProperty("ELS_DB_URL");
        if (!target.endsWith("_test")
                || (appUrl != null && target.equals(databaseName(appUrl)))) {
            throw new IllegalStateException(
                    "Database tests require a separate PostgreSQL database ending in _test");
        }
    }

    private static String databaseName(String url) {
        try {
            if (url != null && url.startsWith("jdbc:postgresql://")) {
                URI uri = URI.create(url.substring(5));
                String path = uri.getPath();
                if (uri.getHost() != null && path != null && path.matches("/[A-Za-z0-9_]+")) {
                    Properties properties = Driver.parseURL(url, new Properties());
                    String database = properties == null ? null
                            : properties.getProperty(PGProperty.PG_DBNAME.getName());
                    if (database != null && database.matches("[A-Za-z0-9_]+")) return database;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Reject malformed targets without including connection details in the error.
        }
        throw new IllegalStateException("Database tests require an explicit PostgreSQL database URL");
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
