# SkillSync

Career and skills platform built with Java 21, Spring Boot 3.5, Spring Security, Thymeleaf, Spring Data JPA, and SQLite for local development. PostgreSQL is configured for deployment. Existing routes and career workflows remain in place.

## Run locally

Requirements: Java 21 and Maven 3.9+.

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
& "C:\Users\s karthik sriram\maven\apache-maven-3.10.0\bin\mvn.cmd" test
& "C:\Users\s karthik sriram\maven\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

Open <http://localhost:8080>. The default datasource is `skillsync-spring.db` in the working directory. Do not remove or replace this file to troubleshoot. Override its location with `SKILLSYNC_DB_URL=jdbc:sqlite:<absolute-path>` if needed. Password reset email remains disabled until configured.

The project includes role paths, skills/evidence, fit scoring, roadmap resources and completion, portfolio, job matching, resume analysis, and a profile-aware rules-based Career Coach. Applications, interview practice, and analytics sections have been removed from the user interface and routing. The coach is not connected to a generative AI provider.

## Database and migrations

Flyway owns schema changes; Hibernate no longer updates the schema. Versioned SQLite migrations are in `src/main/resources/db/migration/sqlite`, and PostgreSQL migrations are in `src/main/resources/db/migration/postgresql`. `V1` creates the current application schema; `V2` adds hashed, expiring password-reset tokens. SQLite uses Flyway plus `ddl-auto=none` because SQLite's identity-column metadata does not match Hibernate's `Long` validation type. PostgreSQL uses `ddl-auto=validate` at startup.

For the existing local SQLite database, the default profile baselines a non-empty schema at version 1 and applies only migration 2. The baseline does not run V1 against existing tables or rewrite their rows. The included test simulates an existing schema with a pre-existing user row, baselines it, applies V2, and verifies the row remains unchanged. The real `skillsync-spring.db` was not used by tests or changed during this work. Before the first restart using the Flyway-enabled app, stop the old process cleanly and make a verified copy/backup of the database. Keep the original untouched until the new app has been checked against it.

PostgreSQL is configured by the `prod` profile. A new, empty database receives V1 and V2 automatically. Existing PostgreSQL installations must first be backed up and confirmed to match the V1 schema; then baseline once at version 1 before normal deployments. Do not enable automatic baselining in production generally. Moving live SQLite data into PostgreSQL is a separate data-transfer task and is not performed by these schema migrations.

## Production configuration

Create a managed PostgreSQL database and a restricted application database user. Supply secrets through the host's secret manager, not source control or command history. PowerShell example (set values only in your deployment environment):

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:SKILLSYNC_DB_URL = "jdbc:postgresql://<host>:5432/<database>"
$env:SKILLSYNC_DB_USERNAME = "<application-user>"
$env:SKILLSYNC_DB_PASSWORD = "<secret-from-secret-store>"
$env:SKILLSYNC_FLYWAY_DB_URL = $env:SKILLSYNC_DB_URL
$env:SKILLSYNC_FLYWAY_DB_USERNAME = "<schema-migration-user>"
$env:SKILLSYNC_FLYWAY_DB_PASSWORD = "<migration-secret-from-secret-store>"
$env:SKILLSYNC_PUBLIC_BASE_URL = "https://<public-host>"
& "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" -jar target\skillsync-0.0.1-SNAPSHOT.jar
```

Build and test before packaging:

```powershell
& "C:\Users\s karthik sriram\maven\apache-maven-3.10.0\bin\mvn.cmd" clean verify
```

For an existing PostgreSQL schema only, after confirming V1 compatibility and taking a restorable backup, run one controlled startup with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=1`; then remove those overrides. Normal production configuration leaves automatic baseline disabled. Review the Flyway history and application logs before routing traffic.

### Password-reset email

Reset links are generated only when `PASSWORD_RESET_EMAIL_ENABLED=true` and an SMTP host, sender address, and mail sender are configured. Set `SMTP_HOST`, `SMTP_PORT` (normally 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_STARTTLS_REQUIRED`, and `SMTP_FROM` in the deployment secret manager. Set `SKILLSYNC_PUBLIC_BASE_URL` to the HTTPS origin. Never commit credentials. The app returns the same public response for known and unknown addresses. Tokens are random, stored only as SHA-256 hashes, valid for 30 minutes, single-use, and invalidate active sessions when consumed. Without SMTP configuration, recovery remains unavailable and no token is issued. No real email was sent during testing.

### Operations and security

- Terminate TLS at a trusted reverse proxy/load balancer; permit forwarded headers only from that proxy. Production session cookies are secure, HTTP-only, and SameSite=Lax.
- The only exposed Actuator endpoint is `/actuator/health`; details are not returned publicly. SMTP is excluded from readiness because mail is optional. Monitor service and database metrics privately without logging credentials, reset tokens, passwords, or pasted resumes.
- Back up PostgreSQL on a schedule, encrypt backups, restrict access, and perform restore drills. Keep a verified pre-migration backup and review Flyway migration output during releases.
- Configure log retention/alerting and database connection limits. Add platform-level request throttling for sign-in, registration, and reset requests before public exposure. Keep reset tokens out of reverse-proxy query-string logs.
- Public registration always creates a student account; it cannot grant admin access by matching a public email. Provision admins out of band only after verifying identity and email ownership, using a restricted database change, and audit that change. Email ownership verification is not implemented for general account registration. Project/certification evidence is user-submitted and is not independently verified by this prototype.
- Session revocation uses the in-process Spring Security session registry. For multiple application replicas, configure a shared session store/registry before relying on reset-triggered revocation across nodes.
- Do not enable debug logging or expose stack traces in production. Do not run `flyway clean` against any user or production database; Flyway clean is disabled in the application configuration.

## Automated checks

`mvn test` uses temporary SQLite databases. Coverage includes fresh migration and JPA startup, legacy-schema baseline/data retention, authentication and student/admin authorization, password hashing and registration validation, skills/profile preferences and career scoring, roadmap persistence, job-description matching and validation, application input validation, reset-token hashing/expiry/single use/session revocation, and generic reset responses. SMTP is replaced by a mocked sender in tests; the test suite never sends email. PostgreSQL migrations are prepared but require a PostgreSQL service to run the production-profile migration/validation test.
