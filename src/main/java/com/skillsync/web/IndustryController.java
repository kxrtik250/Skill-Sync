package com.skillsync.web;

import com.skillsync.user.User;
import com.skillsync.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller public class IndustryController {
    private final UserRepository users;
    public IndustryController(UserRepository users){this.users=users;}
    @GetMapping("/industry-intelligence") String industry(Authentication auth,Model model){User user=users.findByEmail(auth.getName()).orElseThrow();model.addAttribute("user",user);return "industry-intelligence";}
}
