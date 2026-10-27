package com.skillsync.user;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    public static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final ObjectProvider<SessionRegistry> sessionRegistries;
    private final Clock clock;
    private final boolean enabled;
    private final String smtpHost;
    private final String from;
    private final String publicBaseUrl;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailSenders,
            ObjectProvider<SessionRegistry> sessionRegistries, Clock clock,
            @Value("${skillsync.password-reset.enabled:false}") boolean enabled,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${skillsync.mail.from:}") String from,
            @Value("${skillsync.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailSenders = mailSenders;
        this.sessionRegistries = sessionRegistries;
        this.clock = clock;
        this.enabled = enabled;
        this.smtpHost = smtpHost;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    public void requestReset(String email) {
        if (!isConfigured() || email == null || email.isBlank() || email.length() > 254) return;
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) return;

        String token = randomToken();
        Instant now = clock.instant();
        tokens.deleteAllByUserId(user.getId());
        tokens.save(new PasswordResetToken(hash(token), user, now.plus(TOKEN_LIFETIME), now));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Reset your SkillSync password");
        message.setText("Use this one-time link within 30 minutes to reset your SkillSync password:\n\n"
            + publicBaseUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
            + "\n\nIf you did not request this, you can ignore this email.");
        try {
            mailSenders.getObject().send(message);
        } catch (RuntimeException ex) {
            tokens.deleteAllByUserId(user.getId());
            log.warn("Password reset email delivery failed; request response remains generic.");
        }
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.length() > 128 || !isValidNewPassword(newPassword)) return false;
        Instant now = clock.instant();
        PasswordResetToken token = tokens.findByTokenHashAndExpiresAtAfter(hash(rawToken), now).orElse(null);
        if (token == null || tokens.consumeIfValid(token.getId(), now) != 1) return false;

        User user = token.getUser();
        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        tokens.deleteAllByUserId(user.getId());
        sessionRegistries.ifAvailable(registry -> registry.getAllPrincipals().stream()
            .filter(principal -> principal instanceof UserDetails details
                && details.getUsername().equalsIgnoreCase(user.getEmail()))
            .flatMap(principal -> registry.getAllSessions(principal, false).stream())
            .forEach(SessionInformation::expireNow));
        return true;
    }

    public boolean isConfigured() {
        return enabled && !smtpHost.isBlank() && !from.isBlank() && !publicBaseUrl.isBlank()
            && mailSenders.getIfAvailable() != null;
    }

    public static boolean isValidNewPassword(String password) {
        return password != null && password.length() >= 12
            && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
