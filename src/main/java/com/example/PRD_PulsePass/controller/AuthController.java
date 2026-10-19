package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.dto.response.AuthenticatedUserResponse;
import com.example.PRD_PulsePass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
