package com.skillsync.web;

import com.skillsync.user.UserRepository;
import com.skillsync.user.UserSkillRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@Controller public class AdminController {
    private final UserRepository users; private final UserSkillRepository skills; private final RecommendationService careers;
    public AdminController(UserRepository users,UserSkillRepository skills,RecommendationService careers){this.users=users;this.skills=skills;this.careers=careers;}
    @GetMapping("/admin/dashboard") String dashboard(Authentication auth,Model model){model.addAttribute("user",users.findByEmail(auth.getName()).orElseThrow());model.addAttribute("userCount",users.count());model.addAttribute("skillCount",skills.count());model.addAttribute("careerCount",careers.careerNames().size());return "admin-dashboard";}
}
