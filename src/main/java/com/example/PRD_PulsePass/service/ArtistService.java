package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.dto.response.ArtistResponse;
import java.util.List;

public interface ArtistService {
    ArtistResponse findById(Long id);
    ArtistResponse findByStageName(String stageName);
    List<ArtistResponse> findActiveArtists();
}
