package com.example.PRD_PulsePass.integration;

import com.example.PRD_PulsePass.domain.Role;
import com.example.PRD_PulsePass.domain.UserCredential;
import com.example.PRD_PulsePass.domain.UserRole;
import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.dto.response.UserResponse;
import com.example.PRD_PulsePass.repository.UserCredentialRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.repository.UserRoleRepository;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.service.AuthService;
import com.example.PRD_PulsePass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class SecurityIntegrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void atomicRegister_andLoginWithRealDatabase_succeedsAndAssignsUserRole() {
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea_it",
                "andrea_it@email.com",
                "Andrea",
                "Gomez",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1995, 5, 20),
                "FictitiousTestPass2026!"
        );

        UserResponse registered = userService.register(request);
        assertThat(registered.id()).isNotNull();
        assertThat(registered.username()).isEqualTo("andrea_it");

        UserCredential credential = credentialRepository.findByUserId(registered.id()).orElseThrow();
        assertThat(credential.getPasswordHash()).isNotEqualTo("FictitiousTestPass2026!");
        assertThat(passwordEncoder.matches("FictitiousTestPass2026!", credential.getPasswordHash())).isTrue();

        List<UserRole> roles = roleRepository.findByUserId(registered.id());
        assertThat(roles).hasSize(1);
        assertThat(roles.getFirst().getRole()).isEqualTo(Role.USER);

        AuthResponse authResponse = authService.login(new LoginRequest("andrea_it", "FictitiousTestPass2026!"));
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.tokenType()).isEqualTo("Bearer");
        assertThat(authResponse.expiresIn()).isEqualTo(900L);

        Long extractedId = jwtService.extractUserId(authResponse.accessToken());
        assertThat(extractedId).isEqualTo(registered.id());

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea_it", "WrongPass123456!")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
