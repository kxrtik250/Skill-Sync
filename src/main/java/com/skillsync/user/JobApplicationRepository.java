package com.skillsync.user;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface JobApplicationRepository extends JpaRepository<JobApplication,Long>{List<JobApplication> findAllByUserIdOrderByAppliedDateDesc(Long userId);Optional<JobApplication> findByIdAndUserId(Long id,Long userId);}
