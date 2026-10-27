package com.skillsync.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.skillsync.user.PasswordResetService;
import com.skillsync.user.PasswordResetTokenRepository;
import com.skillsync.user.UserRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetUnavailableTest {
    @SuppressWarnings("unchecked")
    @Test
    void disabledEmailRecoveryDoesNotLookUpAccountsOrIssueTokens() {
        UserRepository users = mock(UserRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        ObjectProvider<JavaMailSender> mailers = mock(ObjectProvider.class);
        ObjectProvider<SessionRegistry> sessions = mock(ObjectProvider.class);
        PasswordResetService service = new PasswordResetService(users, tokens,
            mock(PasswordEncoder.class), mailers, sessions, Clock.systemUTC(), false,
            "", "", "http://localhost:8080");

        service.requestReset("member@example.test");

        verify(users, never()).findByEmailIgnoreCase("member@example.test");
        verify(tokens, never()).save(org.mockito.ArgumentMatchers.any());
        verify(mailers, never()).getIfAvailable();
    }
}
