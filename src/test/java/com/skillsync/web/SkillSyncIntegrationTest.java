package com.skillsync.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.skillsync.user.*;
import java.nio.file.Path;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest(properties = {"server.port=0", "spring.thymeleaf.cache=false",
    "skillsync.password-reset.enabled=true", "spring.mail.host=smtp.test.invalid",
    "skillsync.mail.from=no-reply@example.test", "skillsync.public-base-url=https://skillsync.example.test"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SkillSyncIntegrationTest {
    private static final Path DATABASE = createTempDatabase();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE.toAbsolutePath());
        properties.add("spring.flyway.locations", () -> "classpath:db/migration/sqlite");
        properties.add("spring.flyway.baseline-on-migrate", () -> "true");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserSkillRepository skills;
    @Autowired CareerPreferenceRepository preferences;
    @Autowired RoadmapTaskRepository tasks;
    @Autowired JobApplicationRepository applications;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired PasswordResetService passwordResets;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired SessionRegistry sessionRegistry;
    @Autowired RecommendationService recommendations;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void freshDatabaseRunsVersionedMigrationsAndMatchesJpaMappings() {
        assertThat(flyway.info().current().getVersion().getVersion(), org.hamcrest.Matchers.is("2"));
        assertThat(jdbc.queryForObject("select count(*) from sqlite_master where type='table' and name=?",
            Integer.class, "password_reset_tokens"), org.hamcrest.Matchers.is(1));
        assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class), org.hamcrest.Matchers.is(1));
    }

    @Test
    void authenticationAndRoleChecksProtectAdminRoutes() throws Exception {
        User student = saveUser("student");
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/dashboard").with(studentRequest(student))).andExpect(status().isForbidden());
        mvc.perform(post("/api/assistant").with(studentRequest(student)).contentType("application/json")
                .content("{\"message\":\"recommend a path\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(content().string(containsString("UP")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));

        MockHttpSession beforeLogin = new MockHttpSession();
        var login = mvc.perform(post("/login").with(csrf()).session(beforeLogin)
                .param("username", student.getEmail()).param("password", "Integration-Password-2026!"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard")).andReturn();
        MockHttpSession afterLogin = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(afterLogin, org.hamcrest.Matchers.notNullValue());
        assertThat(afterLogin.getId(), org.hamcrest.Matchers.not(beforeLogin.getId()));
        mvc.perform(post("/logout").session(afterLogin).with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/dashboard").session(afterLogin)).andExpect(status().is3xxRedirection());
    }

    @Test
    void registrationHashesPasswordsAndNeverPromotesPublicRegistrations() throws Exception {
        String password = "Integration-Password-2026!";
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Test Student")
                .param("email", "integration-" + UUID.randomUUID() + "@example.test")
                .param("password", password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
        User created = users.findAll().stream().filter(u -> u.getEmail().startsWith("integration-")
            && u.getEmail().endsWith("@example.test")).findFirst().orElseThrow();
        assertThat(created.getAccountRole(), org.hamcrest.Matchers.is("STUDENT"));
        assertThat(created.getPassword(), org.hamcrest.Matchers.not(password));
        assertThat(encoder.matches(password, created.getPassword()), org.hamcrest.Matchers.is(true));

        mvc.perform(post("/register").with(csrf()).param("fullName", "Short")
                .param("email", "bad-password@example.test").param("password", "shortpass"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("12–72")));
    }

    @Test
    void skillsPreferencesCareerScoringAndRoadmapProgressPersist() throws Exception {
        User student = saveUser("career");
        RequestPostProcessor auth = studentRequest(student);
        mvc.perform(post("/skills").with(auth).with(csrf()).param("selected", "Java"))
            .andExpect(status().is3xxRedirection());
        UserSkill java = skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(student.getId()).stream()
            .filter(s -> s.getSkillName().equals("Java")).findFirst().orElseThrow();

        mvc.perform(post("/skills/{id}/details", java.getId()).with(auth).with(csrf())
                .param("proficiency", "ADVANCED").param("confidence", "4")
                .param("verificationStatus", "ASSESSMENT_VERIFIED"))
            .andExpect(status().is3xxRedirection());
        UserSkill reloaded = skills.findById(java.getId()).orElseThrow();
        assertThat(reloaded.getProficiency(), org.hamcrest.Matchers.is(Proficiency.ADVANCED));
        assertThat(reloaded.getVerificationStatus(), org.hamcrest.Matchers.is(VerificationStatus.SELF_REPORTED));

        mvc.perform(post("/skills/{id}/evidence", java.getId()).with(auth).with(csrf())
                .param("type", "ASSESSMENT").param("title", "User-claimed assessment"))
            .andExpect(status().is3xxRedirection());
        assertThat(skills.findById(java.getId()).orElseThrow().getVerificationStatus(),
            org.hamcrest.Matchers.is(VerificationStatus.SELF_REPORTED));

        mvc.perform(post("/skills/{id}/details", java.getId()).with(auth).with(csrf())
                .param("proficiency", "ADVANCED").param("confidence", "9"))
            .andExpect(status().is3xxRedirection());
        assertThat(skills.findById(java.getId()).orElseThrow().getConfidence(), org.hamcrest.Matchers.is(4));

        mvc.perform(post("/profile/preferences").with(auth).with(csrf())
                .param("careerType", "Technical").param("priorities", "Technical depth", "ignored")
                .param("domains", "Cloud", "invalid").param("targetCareer", "Cloud Backend Engineer"))
            .andExpect(status().is3xxRedirection());
        CareerPreference preference = preferences.findByUserId(student.getId()).orElseThrow();
        assertThat(preference.getPriorities(), org.hamcrest.Matchers.is("Technical depth"));
        assertThat(preference.getDomains(), org.hamcrest.Matchers.is("Cloud"));
        int adjustment = recommendations.forUser(student).stream()
            .filter(match -> match.name().equals("Cloud Backend Engineer"))
            .findFirst().orElseThrow().preferenceAdjustment();
        assertThat(adjustment, org.hamcrest.Matchers.is(10));

        mvc.perform(post("/profile").with(auth).with(csrf()).param("fullName", "Invalid CGPA")
                .param("department", "CS").param("cgpa", "10.5"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/profile?error=validation"));
        mvc.perform(get("/dashboard").with(auth)).andExpect(status().isOk());
        mvc.perform(get("/portfolio").with(auth)).andExpect(status().isOk());

        RoadmapTask task = tasks.save(new RoadmapTask(student, "Cloud Backend Engineer", "Docker",
            "LEARN", "Learn Docker", "Test task", 1));
        mvc.perform(post("/roadmap/tasks/{id}", task.getId()).with(auth).with(csrf())
                .param("completed", "true").param("role", "Cloud Backend Engineer"))
            .andExpect(status().is3xxRedirection());
        assertThat(tasks.findById(task.getId()).orElseThrow().isCompleted(), org.hamcrest.Matchers.is(true));
    }

    @Test
    void jobMatchingApplicationsAndResetResponsesAreRealAndIsolated() throws Exception {
        User student = saveUser("jobs");
        RequestPostProcessor auth = studentRequest(student);
        skills.save(new UserSkill(student, "Java", "Programming"));
        mvc.perform(post("/job-matcher").with(auth).with(csrf())
                .param("jobTitle", "Backend developer")
                .param("description", "Build Java services, Spring Boot REST APIs, Docker images, and secure SQL persistence."))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Matched profile skills")))
            .andExpect(content().string(containsString("Java")));
        mvc.perform(post("/job-matcher").with(auth).with(csrf()).param("description", "too short"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("between 40 and 20,000")));

        mvc.perform(post("/forgot-password").with(csrf()).param("email", student.getEmail()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password?submitted"));
        mvc.perform(post("/forgot-password").with(csrf()).param("email", "unknown@example.test"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/forgot-password?submitted"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void passwordResetTokensAreHashedExpiringSingleUseAndRevokeSessions() {
        User student = saveUser("reset");
        org.mockito.ArgumentCaptor<SimpleMailMessage> message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        passwordResets.requestReset(student.getEmail());
        verify(mailSender).send(message.capture());
        String emailBody = message.getValue().getText();
        java.util.regex.Matcher tokenMatcher = java.util.regex.Pattern.compile("token=([A-Za-z0-9_-]{43})")
            .matcher(emailBody);
        assertThat(tokenMatcher.find(), org.hamcrest.Matchers.is(true));
        String rawToken = tokenMatcher.group(1);
        PasswordResetToken stored = resetTokens.findAll().stream()
            .filter(saved -> saved.getUser().getId().equals(student.getId())).findFirst().orElseThrow();
        assertThat(stored.getTokenHash(), org.hamcrest.Matchers.not(rawToken));
        assertThat(stored.getExpiresAt().isAfter(java.time.Instant.now()), org.hamcrest.Matchers.is(true));
        assertThat(stored.getExpiresAt().isBefore(java.time.Instant.now().plusSeconds(31 * 60)),
            org.hamcrest.Matchers.is(true));

        org.springframework.security.core.userdetails.User principal =
            new org.springframework.security.core.userdetails.User(student.getEmail(), "old", java.util.List.of());
        sessionRegistry.registerNewSession("reset-session", principal);
        String newPassword = "New-Integration-Password-2026!";
        assertThat(passwordResets.resetPassword(rawToken, newPassword), org.hamcrest.Matchers.is(true));
        assertThat(encoder.matches(newPassword, users.findById(student.getId()).orElseThrow().getPassword()),
            org.hamcrest.Matchers.is(true));
        assertThat(passwordResets.resetPassword(rawToken, "Another-Password-2026!"), org.hamcrest.Matchers.is(false));
        assertThat(sessionRegistry.getSessionInformation("reset-session").isExpired(), org.hamcrest.Matchers.is(true));

        java.time.Instant now = java.time.Instant.now();
        resetTokens.save(new PasswordResetToken("e".repeat(64), student, now.minusSeconds(1), now.minusSeconds(3601)));
        assertThat(passwordResets.resetPassword("e".repeat(64), "Another-Password-2026!"),
            org.hamcrest.Matchers.is(false));
        assertThat(PasswordResetService.isValidNewPassword("short"), org.hamcrest.Matchers.is(false));
        assertThat(PasswordResetService.isValidNewPassword("Valid-Password-2026!"), org.hamcrest.Matchers.is(true));
    }

    @Test
    void failedSmtpDeliveryDoesNotLeaveAUsableResetToken() {
        User student = saveUser("mailfailure");
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("test-only failure"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        passwordResets.requestReset(student.getEmail());

        assertThat(jdbc.queryForObject("select count(*) from password_reset_tokens where user_id=?",
            Integer.class, student.getId()), org.hamcrest.Matchers.is(0));
    }

    private User saveUser(String prefix) {
        String email = prefix + "-" + UUID.randomUUID() + "@example.test";
        return users.save(new User(email, encoder.encode("Integration-Password-2026!"), "Integration Test"));
    }

    private static RequestPostProcessor studentRequest(User student) {
        return user(student.getEmail()).roles("STUDENT");
    }

    private static Path createTempDatabase() {
        try {
            Path database = java.nio.file.Files.createTempFile("skillsync-integration-", ".db");
            database.toFile().deleteOnExit();
            Path.of(database + "-shm").toFile().deleteOnExit();
            Path.of(database + "-wal").toFile().deleteOnExit();
            return database;
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static <T> void assertThat(T actual, org.hamcrest.Matcher<? super T> matcher) {
        org.hamcrest.MatcherAssert.assertThat(actual, matcher);
    }
}
