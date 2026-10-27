package com.skillsync.user;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CareerPreferenceRepository extends JpaRepository<CareerPreference,Long> { Optional<CareerPreference> findByUserId(Long userId); }
