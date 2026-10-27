package com.skillsync.web;

import com.skillsync.user.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class JobToolsController {
    private final UserRepository users; private final UserSkillRepository skills;
    public JobToolsController(UserRepository users,UserSkillRepository skills){this.users=users;this.skills=skills;}
    private User current(Authentication a){return users.findByEmail(a.getName()).orElseThrow();}
    @GetMapping("/job-matcher") String matcher(Authentication auth,Model model){model.addAttribute("user",current(auth));return "job-matcher";}
    @PostMapping("/job-matcher") String analyze(@RequestParam(required=false) String jobTitle,@RequestParam String description,Authentication auth,Model model){
        User user=current(auth);model.addAttribute("user",user);String text=description==null?"":description.trim();model.addAttribute("jobTitle",jobTitle==null?"":jobTitle.trim());model.addAttribute("jobDescription",text);
        if(text.length()<40||text.length()>20000){model.addAttribute("error","Paste a job description between 40 and 20,000 characters.");return "job-matcher";}
        Map<String,UserSkill> profile=new HashMap<>();skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(user.getId()).forEach(s->profile.put(s.getSkillName().toLowerCase(Locale.ROOT),s));
        List<String> required=RecommendationService.SKILLS.stream().map(RecommendationService.Skill::name).filter(s->Pattern.compile("(?i)(?<![\\p{L}\\p{N}])"+Pattern.quote(s)+"(?![\\p{L}\\p{N}])").matcher(text).find()).toList();
        List<String> matched=new ArrayList<>(),missing=new ArrayList<>();double earned=0;
        for(String skill:required){UserSkill item=profile.get(skill.toLowerCase(Locale.ROOT));if(item==null)missing.add(skill);else{matched.add(skill);earned+=item.getProficiency().getFactor()*item.getVerificationStatus().getFactor()*(0.7+item.getConfidence()*0.06);}}
        int score=required.isEmpty()?0:(int)Math.round(100*earned/required.size());String recommendation=score>=75?"Strong skills alignment":score>=55?"Good skills alignment":score>=30?"Reasonable stretch":"Significant skills gap";
        model.addAttribute("result",new JobMatch(score,required.size(),matched,missing,recommendation,required.isEmpty()));return "job-matcher";
    }
    public record JobMatch(int score,int identifiedSkills,List<String> matched,List<String> missing,String recommendation,boolean noCatalogSkills){}
}
