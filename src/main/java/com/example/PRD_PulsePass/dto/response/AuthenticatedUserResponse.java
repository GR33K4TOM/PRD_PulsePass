package com.example.PRD_PulsePass.dto.response;

import java.util.List;

public record AuthenticatedUserResponse(
    Long userId,
    String username,
    String email,
    List<String> roles
) {}
