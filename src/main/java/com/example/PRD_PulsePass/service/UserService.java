package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.UserResponse;

public interface UserService {
    UserResponse register(RegisterUserRequest request);
    UserResponse findByEmail(String email);
    UserResponse findByUsername(String username);
}
