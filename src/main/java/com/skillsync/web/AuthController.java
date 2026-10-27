package com.skillsync.web;

import com.skillsync.user.PasswordResetService;
import com.skillsync.user.User;
import com.skillsync.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private static final String RESET_MESSAGE = "If an account matches that email and password recovery is available, "
        + "a reset link will be sent shortly. Check your inbox, or ignore this message if no account matches.";
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordResetService passwordResets;

    public AuthController(UserRepository users, PasswordEncoder encoder, PasswordResetService passwordResets) {
        this.users = users;
        this.encoder = encoder;
        this.passwordResets = passwordResets;
    }

    @GetMapping({"/", "/login"}) String login() { return "login"; }
    @GetMapping("/register") String register() { return "register"; }

    @PostMapping("/register")
    String create(@RequestParam String fullName, @RequestParam String email,
            @RequestParam String password, Model model) {
        String name = fullName == null ? "" : fullName.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (name.isBlank() || name.length() > 100
                || !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || cleanEmail.length() > 254
                || !PasswordResetService.isValidNewPassword(password)) {
            model.addAttribute("error", "Enter a valid name and email. Use a password with 12–72 UTF-8 bytes.");
            return "register";
        }
        if (users.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            model.addAttribute("error", "An account already exists for this email.");
            return "register";
        }
        // Public registration can never grant a privileged role. Provision administrators out of band.
        users.save(new User(cleanEmail, encoder.encode(password), name));
        return "redirect:/login?registered";
    }

    @GetMapping("/forgot-password")
    String forgotPassword(@RequestParam(required = false) String submitted, Model model) {
        model.addAttribute("submitted", submitted != null);
        model.addAttribute("emailRecoveryConfigured", passwordResets.isConfigured());
        model.addAttribute("resetMessage", RESET_MESSAGE);
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    String requestReset(@RequestParam(required = false) String email) {
        passwordResets.requestReset(email);
        return "redirect:/forgot-password?submitted";
    }

    @GetMapping("/reset-password")
    String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    String resetPassword(@RequestParam(required = false) String token,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String confirmation) {
        if (password == null || !password.equals(confirmation)
                || !passwordResets.resetPassword(token, password)) {
            return "redirect:/reset-password?invalid";
        }
        return "redirect:/login?passwordReset";
    }
}
