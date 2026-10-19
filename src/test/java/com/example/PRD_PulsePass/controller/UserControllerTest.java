package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.UserResponse;
import com.example.PRD_PulsePass.exception.DuplicateResourceException;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.security.CustomUserDetailsService;
import com.example.PRD_PulsePass.security.JwtAuthenticationFilter;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.security.RestAccessDeniedHandler;
import com.example.PRD_PulsePass.security.RestAuthenticationEntryPoint;
import com.example.PRD_PulsePass.security.SecurityConfig;
import com.example.PRD_PulsePass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testCtrlUsr001_registrarValido_returns201() throws Exception {
        UserResponse response = new UserResponse(
                1L, "andrea", "andrea@email.com", true, "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1995, 5, 20)
        );
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(response);

        String json = """
                {
                    "username": "andrea",
                    "email": "andrea@email.com",
                    "firstName": "Andrea",
                    "lastName": "Gomez",
                    "phone": "3001234567",
                    "city": "Santa Marta",
                    "birthDate": "1995-05-20",
                    "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    void testCtrlUsr002_emailInvalido_returns400() throws Exception {
        String invalidJson = """
                {
                    "username": "andrea",
                    "email": "invalid-email-format",
                    "firstName": "Andrea",
                    "lastName": "Gomez",
                    "phone": "3001234567",
                    "city": "Santa Marta",
                    "birthDate": "1995-05-20",
                    "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").exists());

        verify(userService, never()).register(any());
    }

    @Test
    void testCtrlUsr003_usernameDuplicado_returns409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists."));

        String json = """
                {
                    "username": "andrea",
                    "email": "andrea@email.com",
                    "firstName": "Andrea",
                    "lastName": "Gomez",
                    "phone": "3001234567",
                    "city": "Santa Marta",
                    "birthDate": "1995-05-20",
                    "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists."));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlUsr004_consultarPorEmail_returns200() throws Exception {
        UserResponse response = new UserResponse(
                1L, "andrea", "andrea@email.com", true, "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1995, 5, 20)
        );
        when(userService.findByEmail("andrea@email.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@email.com"));

        verify(userService).findByEmail("andrea@email.com");
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlUsr005_consultarPorUsername_returns200() throws Exception {
        UserResponse response = new UserResponse(
                1L, "andrea", "andrea@email.com", true, "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1995, 5, 20)
        );
        when(userService.findByUsername("andrea")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }
}
