package com.skillsync.user;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CareerProjectRepository extends JpaRepository<CareerProject,Long>{
    List<CareerProject> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CareerProject> findByIdAndUserId(Long id,Long userId);
}
