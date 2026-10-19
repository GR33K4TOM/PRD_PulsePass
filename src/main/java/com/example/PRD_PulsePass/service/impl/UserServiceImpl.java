package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Role;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.domain.UserCredential;
import com.example.PRD_PulsePass.domain.UserProfile;
import com.example.PRD_PulsePass.domain.UserRole;
import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.UserResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.DuplicateResourceException;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.UserMapper;
import com.example.PRD_PulsePass.repository.UserCredentialRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.repository.UserRoleRepository;
import com.example.PRD_PulsePass.service.AuthorizationService;
import com.example.PRD_PulsePass.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this(userRepository, userMapper, null, null, null, null);
    }

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            UserCredentialRepository credentialRepository,
            UserRoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthorizationService authorizationService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (userRepository.findAll().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(request.username()))) {
            throw new DuplicateResourceException("Username already exists.");
        }
        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new DuplicateResourceException("Email already exists.");
        }
        if (request.birthDate() != null && request.birthDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Birth date cannot be in the future.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setActive(true);

        UserProfile profile = new UserProfile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setCity(request.city());
        profile.setBirthDate(request.birthDate());

        profile.setUser(user);
        user.setProfile(profile);

        User saved = userRepository.save(user);

        if (credentialRepository != null && passwordEncoder != null && request.password() != null) {
            UserCredential credential = new UserCredential();
            credential.setUser(saved);
            credential.setPasswordHash(passwordEncoder.encode(request.password()));
            credential.setPasswordChangedAt(LocalDateTime.now());
            credentialRepository.save(credential);
        }

        if (roleRepository != null && saved != null && saved.getId() != null) {
            UserRole userRole = new UserRole(saved.getId(), Role.USER);
            roleRepository.save(userRole);
        }

        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (authorizationService != null) {
            authorizationService.checkCanReadUser(user);
        }
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse findByUsername(String username) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        if (authorizationService != null) {
            authorizationService.checkCanReadUser(user);
        }
        return userMapper.toResponse(user);
    }
}
