package com.skillsync.web;

import com.skillsync.user.UserSkill;
import com.skillsync.user.UserSkillRepository;
import com.skillsync.user.User;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReadinessService {
    private final UserSkillRepository skills;
    private final RecommendationService recommendations;
    public ReadinessService(UserSkillRepository skills,RecommendationService recommendations){this.skills=skills;this.recommendations=recommendations;}
    public Readiness forUser(User user){
        List<UserSkill> profile=skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(user.getId());
        int skill=profile.isEmpty()?0:(int)Math.round(profile.stream().mapToDouble(UserSkill::getConfidenceScore).average().orElse(0));
        int evidence=profile.isEmpty()?0:(int)Math.round(profile.stream().filter(s->!s.getEvidence().isEmpty()).count()*100.0/profile.size());
        int alignment=recommendations.forUser(user).getFirst().score();
        int total=(int)Math.round((skill+evidence+alignment)/3.0);
        String formula="Overall is the mean of skill strength, evidence coverage, and best career fit. It is an exploratory signal, not an employment prediction.";
        return new Readiness(total,skill,evidence,alignment,formula);
    }
    public record Readiness(int overall,int skills,int evidence,int alignment,String explanation){}
}
