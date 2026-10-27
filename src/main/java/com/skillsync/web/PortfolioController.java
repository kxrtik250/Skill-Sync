package com.skillsync.web;

import com.skillsync.user.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PortfolioController {
    private final UserRepository users; private final CareerProjectRepository projects; private final UserSkillRepository skills; private final RecommendationService recommendations; private final SkillProfileService skillProfiles;
    public PortfolioController(UserRepository users,CareerProjectRepository projects,UserSkillRepository skills,RecommendationService recommendations,SkillProfileService skillProfiles){this.users=users;this.projects=projects;this.skills=skills;this.recommendations=recommendations;this.skillProfiles=skillProfiles;}
    private User current(Authentication auth){return users.findByEmail(auth.getName()).orElseThrow();}
    @GetMapping("/projects") String projects(Authentication auth,Model model,@RequestParam(required=false) String error){User user=current(auth);List<RecommendationService.Recommendation> roles=recommendations.forUser(user);model.addAttribute("user",user);model.addAttribute("inventory",skillProfiles.forUser(user));model.addAttribute("projects",projects.findAllByUserIdOrderByCreatedAtDesc(user.getId()));model.addAttribute("recommendations",roles);model.addAttribute("recommendedProject",roles.isEmpty()?null:idea(roles.getFirst()));model.addAttribute("error",error);return "projects";}
    private String idea(RecommendationService.Recommendation role){return switch(role.domain()){case "Cloud"->"Deploy a secure, containerized REST API";case "AI"->"Build and evaluate an AI-powered feature";case "Data"->"Create a reproducible analytics pipeline";case "Cybersecurity"->"Run a defensive threat-model and remediation lab";case "Design"->"Conduct a user study and prototype";case "Product"->"Deliver a product discovery case study";case "Marketing"->"Plan and measure a content campaign";default->"Build a portfolio-ready role case study";};}
    @PostMapping("/projects") String addProject(@RequestParam String name,@RequestParam(required=false) String description,@RequestParam(required=false) String technologies,@RequestParam(required=false) String[] demonstratedSkills,@RequestParam(required=false) String githubUrl,@RequestParam(required=false) String demoUrl,@RequestParam(required=false) String duration,@RequestParam(required=false) String role,@RequestParam(required=false) String outcome,@RequestParam(required=false) String projectType,Authentication auth){
        User user=current(auth);String g=cleanUrl(githubUrl),d=cleanUrl(demoUrl);if(name.isBlank()||name.length()>120||g==null||d==null)return "redirect:/projects?error=validation";
        Map<String,UserSkill> known=new HashMap<>();skills.findAllByUserIdOrderByCategoryAscSkillNameAsc(user.getId()).forEach(s->known.put(s.getSkillName(),s));LinkedHashSet<String> selected=new LinkedHashSet<>();if(demonstratedSkills!=null)for(String n:demonstratedSkills)if(known.containsKey(n))selected.add(n);
        String skillList=String.join(",",selected);CareerProject project=projects.save(new CareerProject(user,name.trim(),safe(description,2000),safe(technologies,800),skillList,g,d,safe(duration,80),safe(role,100),safe(outcome,1000),safe(projectType,60)));
        for(String n:selected){UserSkill skill=known.get(n);skill.addEvidence(new SkillEvidence(EvidenceType.PROJECT,project.getName(),"Project portfolio entry"+(project.getDescription().isBlank()?"":": "+project.getDescription()),g.isBlank()?d:g));skill.setVerificationStatus(VerificationStatus.PROJECT_BACKED);skills.save(skill);}
        return "redirect:/projects?saved";
    }
    private String cleanUrl(String value){String v=safe(value,500);return v.isBlank()||(v.length()<=255&&v.matches("https?://[^\\s]+"))?v:null;}
    private String safe(String value,int max){if(value==null)return "";String v=value.trim();return v.length()>max?v.substring(0,max):v;}
    @PostMapping("/projects/{id}/featured") String feature(@PathVariable Long id,Authentication auth){User user=current(auth);CareerProject project=projects.findByIdAndUserId(id,user.getId()).orElseThrow();project.setFeatured(!project.isFeatured());projects.save(project);return "redirect:/portfolio";}
    @GetMapping("/portfolio") String portfolio(Authentication auth,Model model){User user=current(auth);List<CareerProject> mine=projects.findAllByUserIdOrderByCreatedAtDesc(user.getId());List<UserSkill> inventory=skillProfiles.forUser(user);model.addAttribute("user",user);model.addAttribute("projects",mine);model.addAttribute("inventory",inventory);model.addAttribute("evidenceCount",inventory.stream().mapToInt(s->s.getEvidence().size()).sum());model.addAttribute("verifiedSkills",inventory.stream().filter(s->!s.getEvidence().isEmpty()).count());return "portfolio";}
}
