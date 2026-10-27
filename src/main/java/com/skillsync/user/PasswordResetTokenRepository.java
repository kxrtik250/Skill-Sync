package com.skillsync.user;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
    @Transactional
    void deleteAllByUserId(Long userId);

    @Modifying
    @Query("delete from PasswordResetToken t where t.id = :id and t.expiresAt > :now")
    int consumeIfValid(@Param("id") Long id, @Param("now") Instant now);
}
