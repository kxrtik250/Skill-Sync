package com.skillsync.web;

import com.skillsync.user.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final UserRepository users;
    private final RecommendationService recommendations;
    private final SkillProfileService profiles;
    private final ReadinessService readiness;
    private final CareerPreferenceRepository preferences;
    private final RoadmapTaskRepository roadmapTasks;
    public DashboardController(UserRepository users,RecommendationService recommendations,SkillProfileService profiles,ReadinessService readiness,CareerPreferenceRepository preferences,RoadmapTaskRepository roadmapTasks){this.users=users;this.recommendations=recommendations;this.profiles=profiles;this.readiness=readiness;this.preferences=preferences;this.roadmapTasks=roadmapTasks;}

    @GetMapping("/dashboard") String dashboard(Authentication auth,Model model){
        User user=users.findByEmail(auth.getName()).orElseThrow();
        List<UserSkill> inventory=profiles.forUser(user);
        List<RecommendationService.Recommendation> ranked=recommendations.forUser(user),top=ranked.stream().limit(3).toList();
        Map<String,Long> gaps=top.stream().flatMap(r->r.gaps().stream()).collect(Collectors.groupingBy(s->s,Collectors.counting()));
        List<String> biggestGaps=gaps.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey)).limit(5).map(Map.Entry::getKey).toList();
        LocalDate weekStart=LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long practiced=inventory.stream().filter(s->s.getLastPracticed()!=null&&!s.getLastPracticed().isBefore(weekStart)).count();
        long projects=inventory.stream().flatMap(s->s.getEvidence().stream()).filter(e->!e.getCreatedAt().toLocalDate().isBefore(weekStart)&&e.getType()==EvidenceType.PROJECT).count();
        List<RoadmapTask> savedTasks=roadmapTasks.findAllByUserId(user.getId());
        CareerPreference goal=preferences.findByUserId(user.getId()).orElse(null);
        int goalMatch=goal==null||goal.getTargetCareer()==null?0:ranked.stream().filter(r->r.name().equals(goal.getTargetCareer())).mapToInt(RecommendationService.Recommendation::score).findFirst().orElse(0);
        model.addAttribute("user",user);model.addAttribute("skillCount",inventory.size());model.addAttribute("readiness",readiness.forUser(user));
        model.addAttribute("careerCount",ranked.size());model.addAttribute("recommendations",top);model.addAttribute("biggestGaps",biggestGaps);
        model.addAttribute("nextSkill",top.isEmpty()||top.getFirst().gaps().isEmpty()?null:top.getFirst().gaps().getFirst());model.addAttribute("nextCareer",top.isEmpty()?null:top.getFirst().name());
        model.addAttribute("practicedThisWeek",practiced);model.addAttribute("projectsThisWeek",projects);model.addAttribute("roadmapTaskCount",savedTasks.size());model.addAttribute("roadmapCompletedCount",savedTasks.stream().filter(RoadmapTask::isCompleted).count());model.addAttribute("goal",goal);model.addAttribute("goalMatch",goalMatch);
        return "dashboard";
    }
}
