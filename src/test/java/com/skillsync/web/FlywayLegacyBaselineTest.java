package com.skillsync.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlywayLegacyBaselineTest {
    @TempDir Path temporaryDirectory;

    @Test
    void baseliningExistingSchemaKeepsRowsAndOnlyAppliesNewerMigration() throws Exception {
        Path database = temporaryDirectory.resolve("legacy.sqlite");
        String url = "jdbc:sqlite:" + database.toAbsolutePath();

        // Approximate the old Hibernate-created tables: integer IDs and no declared foreign keys.
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.executeUpdate("create table users (id integer primary key, email varchar(255) "
                + "not null unique, password varchar(255) not null, full_name varchar(255), "
                + "department varchar(255), cgpa float, skills varchar(3000), account_role varchar(20))");
            statement.executeUpdate("create table user_skills (id integer primary key, category varchar(255) "
                + "not null, confidence integer not null, last_practiced date, proficiency varchar(255) "
                + "not null, skill_name varchar(255) not null, verification_status varchar(255) "
                + "not null, user_id bigint not null)");
            statement.executeUpdate("insert into users(id,email,password,full_name,account_role) "
                + "values (1,'legacy@example.test','already-hashed','Existing user','STUDENT')");
            statement.executeUpdate("insert into user_skills(id,category,confidence,proficiency,skill_name,"
                + "verification_status,user_id) values (7,'Programming',4,'ADVANCED','Java','SELF_REPORTED',1)");
        }

        Flyway migrated = Flyway.configure().dataSource(url, null, null)
            .locations("classpath:db/migration/sqlite").baselineOnMigrate(true).baselineVersion("1").load();
        migrated.migrate();

        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select id,email,password from users where id=1")) {
            assertEquals(true, result.next());
            assertEquals("legacy@example.test", result.getString("email"));
            assertEquals("already-hashed", result.getString("password"));
        }
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from user_skills where id=7 and skill_name='Java'")) {
            assertEquals(true, result.next());
            assertEquals(1, result.getInt(1));
        }
        assertEquals("2", migrated.info().current().getVersion().getVersion());
    }
}
