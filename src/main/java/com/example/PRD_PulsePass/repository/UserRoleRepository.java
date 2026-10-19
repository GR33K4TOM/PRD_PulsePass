package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.UserRole;
import com.example.PRD_PulsePass.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(Long userId);
}
