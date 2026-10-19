package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUserId(Long userId);
}
