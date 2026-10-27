package com.skillsync.web;

import com.skillsync.user.User;
import com.skillsync.user.UserRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class CareerAssistantController {
    private final UserRepository users; private final RecommendationService recommendations;
    public CareerAssistantController(UserRepository users,RecommendationService recommendations){this.users=users;this.recommendations=recommendations;}
    @PostMapping public Reply answer(@RequestBody Question question,Authentication auth){
        User user=users.findByEmail(auth.getName()).orElseThrow(); List<RecommendationService.Recommendation> ranked=recommendations.forUser(user); String prompt=question.message()==null?"":question.message().toLowerCase(); RecommendationService.Recommendation top=ranked.getFirst();
        if(prompt.contains("nontechnical")||prompt.contains("non-technical")||prompt.contains("business")){var role=ranked.stream().filter(r->r.type().equals("Nontechnical")).findFirst().orElse(top);return new Reply("For nontechnical and business-facing work, start with "+role.name()+" ("+role.score()+"% skill match). Build next: "+shortList(role.gaps())+". Browse Career Paths to compare all options.","/career-paths");}
        if(prompt.contains("roadmap")||prompt.contains("learn")||prompt.contains("next")){String gap=top.gaps().isEmpty()?"an advanced portfolio project":top.gaps().getFirst();return new Reply("Your strongest current match is "+top.name()+" at "+top.score()+"%. The roadmap estimates one week per missing skill. Start with "+gap+" and use the linked course or official guide, then demonstrate it in a small project.","/roadmap?role="+java.net.URLEncoder.encode(top.name(),java.nio.charset.StandardCharsets.UTF_8));}
        if(prompt.contains("skill")||prompt.contains("improve")||prompt.contains("gap")){return new Reply("Your strongest match is "+top.name()+" at "+top.score()+"%. The most useful gaps to close are "+shortList(top.gaps())+". Add only skills you can demonstrate, then compare the recalculated paths.","/skills");}
        if(prompt.contains("industry")||prompt.contains("future")||prompt.contains("trend")){return new Reply("Current skill signals in this project include AI and big data, cybersecurity, technological literacy, analytical thinking, and collaboration. Sources and context are linked on Career Paths. Treat these as broad trend signals, not a live vacancy forecast.","/career-paths");}
        return new Reply("I can help compare career directions, identify skill gaps, suggest what to learn next, or explain industry skill signals. Try asking “Which nontechnical career fits me?” or “What should I learn next?”", "/career-paths");
    }
    private String shortList(List<String> values){return values.isEmpty()?"no core gaps remain":String.join(", ",values.stream().limit(4).toList());}
    public record Question(String message){} public record Reply(String answer,String link){}
}
