package com.example.PRD_PulsePass.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String email, String passwordHash, boolean active, List<GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
