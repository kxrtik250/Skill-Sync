package com.skillsync.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionDatabaseConfigurationGuardTest {
    @Test
    void rejectsMissingDatabaseSettingsBeforeAConnectionCanBeAttempted() {
        MockEnvironment environment = new MockEnvironment();

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> ProductionDatabaseConfigurationGuard.validate(environment));

        assertTrue(error.getMessage().contains("SKILLSYNC_DB_URL"));
        assertTrue(error.getMessage().contains("No database connection was attempted"));
    }

    @Test
    void rejectsSqliteUrlInProduction() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SKILLSYNC_DB_URL", "jdbc:sqlite:skillsync.db")
            .withProperty("SKILLSYNC_DB_USERNAME", "test-user")
            .withProperty("SKILLSYNC_DB_PASSWORD", "test-password");

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> ProductionDatabaseConfigurationGuard.validate(environment));

        assertTrue(error.getMessage().contains("prod profile requires jdbc:postgresql://"));
    }

    @Test
    void allowsCompletePostgresSettingsAndOptionalSeparateFlywayCredentials() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SKILLSYNC_DB_URL", "jdbc:postgresql://127.0.0.1:55432/skillsync_test")
            .withProperty("SKILLSYNC_DB_USERNAME", "skillsync_test")
            .withProperty("SKILLSYNC_DB_PASSWORD", "disposable-test-secret");

        assertDoesNotThrow(() -> ProductionDatabaseConfigurationGuard.validate(environment));
    }
}
