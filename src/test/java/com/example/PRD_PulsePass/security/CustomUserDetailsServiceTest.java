package com.example.PRD_PulsePass.security;

import com.example.PRD_PulsePass.domain.Role;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.domain.UserCredential;
import com.example.PRD_PulsePass.domain.UserRole;
import com.example.PRD_PulsePass.repository.UserCredentialRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private UserRoleRepository roleRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_existingActiveUser_returnsPrincipalWithRoles() {
        User user = new User();
        user.setId(1L);
        user.setUsername("andrea");
        user.setEmail("andrea@email.com");
        user.setActive(true);

        UserCredential cred = new UserCredential();
        cred.setUserId(1L);
        cred.setPasswordHash("$2a$10$hash");

        UserRole roleUser = new UserRole(1L, Role.USER);
        UserRole roleStaff = new UserRole(1L, Role.STAFF);

        when(userRepository.findByUsernameIgnoreCase("andrea")).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(1L)).thenReturn(Optional.of(cred));
        when(roleRepository.findByUserId(1L)).thenReturn(List.of(roleUser, roleStaff));

        SecurityPrincipal principal = (SecurityPrincipal) userDetailsService.loadUserByUsername("andrea");

        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getEmail()).isEqualTo("andrea@email.com");
        assertThat(principal.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    void loadUserByUsername_nonExistingUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserById_inactiveUser_returnsDisabledPrincipal() {
        User user = new User();
        user.setId(2L);
        user.setUsername("miguel");
        user.setEmail("miguel@email.com");
        user.setActive(false);

        UserCredential cred = new UserCredential();
        cred.setUserId(2L);
        cred.setPasswordHash("$2a$10$hash");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(credentialRepository.findByUserId(2L)).thenReturn(Optional.of(cred));
        when(roleRepository.findByUserId(2L)).thenReturn(List.of(new UserRole(2L, Role.USER)));

        SecurityPrincipal principal = userDetailsService.loadUserById(2L);

        assertThat(principal.isEnabled()).isFalse();
    }
}
