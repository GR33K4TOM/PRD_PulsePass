package com.example.PRD_PulsePass.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {}
