package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.dto.response.AuthenticatedUserResponse;
import com.example.PRD_PulsePass.dto.response.UserResponse;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.security.SecurityPrincipal;
import com.example.PRD_PulsePass.service.AuthService;
import com.example.PRD_PulsePass.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserService userService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
            );
            SecurityPrincipal principal = (SecurityPrincipal) auth.getPrincipal();
            String token = jwtService.generateToken(principal.getUserId());
            log.info("Successful login for userId={}", principal.getUserId());
            return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds());
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for identifier={}", request.identifier());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        return userService.register(request);
    }

    @Override
    public AuthenticatedUserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityPrincipal sp) {
            List<String> roles = sp.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            return new AuthenticatedUserResponse(
                    sp.getUserId(),
                    sp.getUsername(),
                    sp.getEmail(),
                    roles
            );
        }
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new AuthenticatedUserResponse(
                null,
                auth.getName(),
                auth.getName(),
                roles
        );
    }
}
