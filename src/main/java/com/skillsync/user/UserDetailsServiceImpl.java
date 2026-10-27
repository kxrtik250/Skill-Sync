package com.skillsync.user;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository users;
    public UserDetailsServiceImpl(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        String role = user.getAccountRole();
        if (!List.of("ADMIN", "STUDENT").contains(role)) role = "STUDENT";
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),
            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
