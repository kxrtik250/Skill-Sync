package com.skillsync.user;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoadmapTaskRepository extends JpaRepository<RoadmapTask,Long>{
    List<RoadmapTask> findAllByUserIdAndCareerNameOrderByWeekIndexAscIdAsc(Long userId,String careerName);
    List<RoadmapTask> findAllByUserId(Long userId);
    Optional<RoadmapTask> findByIdAndUserId(Long id,Long userId);
    Optional<RoadmapTask> findByUserIdAndCareerNameAndSkillNameAndPhase(Long userId,String careerName,String skillName,String phase);
}
