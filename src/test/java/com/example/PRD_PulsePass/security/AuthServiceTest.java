package com.example.PRD_PulsePass.security;

import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.service.UserService;
import com.example.PRD_PulsePass.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_validCredentials_emitsTokenAndMetadata() {
        SecurityPrincipal principal = new SecurityPrincipal(
                10L, "andrea", "andrea@email.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken authResult =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(jwtService.generateToken(10L)).thenReturn("signed-jwt-token");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("andrea", "UnaFraseSeguraLarga2026!"));

        assertThat(response.accessToken()).isEqualTo("signed-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(jwtService).generateToken(10L);
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsAndNeverEmitsToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_inactiveUser_throwsBadCredentialsAndNeverEmitsToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("miguel", "UnaFraseSeguraLarga2026!")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
