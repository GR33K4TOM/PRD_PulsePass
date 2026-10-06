package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByCode(String code);
}
