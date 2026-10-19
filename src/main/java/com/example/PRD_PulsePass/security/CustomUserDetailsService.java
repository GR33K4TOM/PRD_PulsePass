package com.example.PRD_PulsePass.security;

import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.domain.UserCredential;
import com.example.PRD_PulsePass.domain.UserRole;
import com.example.PRD_PulsePass.repository.UserCredentialRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.repository.UserRoleRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserCredentialRepository credentialRepository,
            UserRoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return buildPrincipal(user);
    }

    public SecurityPrincipal loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return buildPrincipal(user);
    }

    private SecurityPrincipal buildPrincipal(User user) {
        String passwordHash = credentialRepository.findByUserId(user.getId())
                .map(UserCredential::getPasswordHash)
                .orElseThrow(() -> new UsernameNotFoundException("User credentials not configured"));

        List<UserRole> userRoles = roleRepository.findByUserId(user.getId());
        List<GrantedAuthority> authorities = userRoles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.getRole().name()))
                .toList();

        boolean active = Boolean.TRUE.equals(user.getActive());

        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                passwordHash,
                active,
                authorities
        );
    }
}
