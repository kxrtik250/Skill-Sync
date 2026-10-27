package com.skillsync.user;

import com.skillsync.web.RecommendationService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillProfileService {
    private final UserSkillRepository skills;
    public SkillProfileService(UserSkillRepository skills){this.skills=skills;}
    @Transactional
    public List<UserSkill> forUser(User user){
        if(!user.getSkills().isBlank()) for(String raw:user.getSkills().split(",")){
            String name=raw.trim();if(name.isBlank()||skills.findByUserIdAndSkillNameIgnoreCase(user.getId(),name).isPresent())continue;
            RecommendationService.SKILLS.stream().filter(s->s.name().equalsIgnoreCase(name)).findFirst()
                .ifPresent(item->skills.save(new UserSkill(user,item.name(),item.category())));
        }
        return skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(user.getId());
    }
    @Transactional
    public void addSkills(User user,String[] selected){
        Set<String> valid=new LinkedHashSet<>();if(selected!=null)valid.addAll(Arrays.asList(selected));
        for(RecommendationService.Skill item:RecommendationService.SKILLS)if(valid.contains(item.name())&&skills.findByUserIdAndSkillNameIgnoreCase(user.getId(),item.name()).isEmpty())skills.save(new UserSkill(user,item.name(),item.category()));
        LinkedHashSet<String> all=new LinkedHashSet<>();for(UserSkill skill:forUser(user))all.add(skill.getSkillName());user.setSkills(String.join(",",all));
    }
}
