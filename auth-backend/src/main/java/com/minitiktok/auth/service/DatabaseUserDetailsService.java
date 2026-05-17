package com.minitiktok.auth.service;

import com.minitiktok.auth.entity.User;
import com.minitiktok.auth.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new AuthUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getEnabled()));
    }
}
