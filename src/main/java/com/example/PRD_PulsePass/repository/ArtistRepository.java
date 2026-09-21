package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
