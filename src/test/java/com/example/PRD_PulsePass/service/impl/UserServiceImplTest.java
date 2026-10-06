package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.dto.request.RegisterUserRequest;
import com.example.PRD_PulsePass.dto.response.UserResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.DuplicateResourceException;
import com.example.PRD_PulsePass.mapper.UserMapper;
import com.example.PRD_PulsePass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_validUser_returnsDto() {
        RegisterUserRequest req = new RegisterUserRequest("user1", "user@test.com", "First", "Last", "123", "City", LocalDate.of(1990, 1, 1));
        User saved = new User();
        UserResponse res = new UserResponse(1L, "user1", "user@test.com", true, "First", "Last", "123", "City", LocalDate.of(1990, 1, 1));
        
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(res);

        UserResponse result = userService.register(req);
        
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("user1");
    }

    @Test
    void register_duplicatedUsername_throwsDuplicate() {
        RegisterUserRequest req = new RegisterUserRequest("user1", "user@test.com", "First", "Last", "123", "City", LocalDate.of(1990, 1, 1));
        User existing = new User();
        existing.setUsername("user1");
        
        when(userRepository.findAll()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_duplicatedEmail_throwsDuplicate() {
        RegisterUserRequest req = new RegisterUserRequest("user2", "user@test.com", "First", "Last", "123", "City", LocalDate.of(1990, 1, 1));
        User existing = new User();
        
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByEmailIgnoreCase("user@test.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void register_futureBirthDate_throwsBusinessRule() {
        RegisterUserRequest req = new RegisterUserRequest("user3", "user3@test.com", "First", "Last", "123", "City", LocalDate.now().plusDays(10));
        
        when(userRepository.findAll()).thenReturn(List.of());
        when(userRepository.findByEmailIgnoreCase("user3@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(BusinessRuleException.class);
    }
}
