package com.skillsync.user;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InterviewAttemptRepository extends JpaRepository<InterviewAttempt,Long>{List<InterviewAttempt> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);List<InterviewAttempt> findAllByUserId(Long userId);}
