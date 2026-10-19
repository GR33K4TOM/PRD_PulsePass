package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.dto.response.AuthenticatedUserResponse;
import com.example.PRD_PulsePass.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserResponse register(RegisterUserRequest request);
    AuthenticatedUserResponse getCurrentUser();
}
