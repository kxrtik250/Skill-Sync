package com.skillsync.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/** Fails before Flyway or the datasource can connect when production DB settings are incomplete. */
public final class ProductionDatabaseConfigurationGuard implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                validate(environment);
                return;
            }
        }
    }

    static void validate(ConfigurableEnvironment environment) {
        List<String> missing = new ArrayList<>();
        String url = required(environment, "SKILLSYNC_DB_URL", missing);
        required(environment, "SKILLSYNC_DB_USERNAME", missing);
        required(environment, "SKILLSYNC_DB_PASSWORD", missing);

        String flywayUrl = environment.getProperty("SKILLSYNC_FLYWAY_DB_URL");
        if (flywayUrl != null && !flywayUrl.isBlank() && !flywayUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Invalid SKILLSYNC_FLYWAY_DB_URL: production requires jdbc:postgresql://");
        }
        requiredIfPresent(environment, "SKILLSYNC_FLYWAY_DB_USERNAME", missing);
        requiredIfPresent(environment, "SKILLSYNC_FLYWAY_DB_PASSWORD", missing);

        if (url != null && !url.isBlank() && !url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Invalid SKILLSYNC_DB_URL: the prod profile requires jdbc:postgresql://; SQLite is supported only by the local profile.");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Production database configuration is incomplete. Set non-empty "
                + String.join(", ", missing) + ". No database connection was attempted.");
        }
    }

    private static String required(ConfigurableEnvironment environment, String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) missing.add(key);
        return value;
    }

    private static void requiredIfPresent(ConfigurableEnvironment environment, String key, List<String> missing) {
        String value = environment.getProperty(key);
        if (value != null && value.isBlank()) missing.add(key);
    }
}
