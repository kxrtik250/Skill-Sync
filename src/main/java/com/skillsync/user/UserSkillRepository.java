package com.skillsync.user;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserSkillRepository extends JpaRepository<UserSkill,Long> {
    @EntityGraph(attributePaths = "evidence")
    List<UserSkill> findAllByUserIdOrderByCategoryAscSkillNameAsc(Long userId);
    @EntityGraph(attributePaths = "evidence")
    Optional<UserSkill> findByIdAndUserId(Long id,Long userId);
    Optional<UserSkill> findByUserIdAndSkillNameIgnoreCase(Long userId,String skillName);
}
