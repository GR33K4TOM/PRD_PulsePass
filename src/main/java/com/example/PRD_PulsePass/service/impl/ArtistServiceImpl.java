package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.ArtistMapper;
import com.example.PRD_PulsePass.repository.ArtistRepository;
import com.example.PRD_PulsePass.service.ArtistService;
import com.example.PRD_PulsePass.dto.response.ArtistResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final ArtistMapper artistMapper;

    public ArtistServiceImpl(ArtistRepository artistRepository, ArtistMapper artistMapper) {
        this.artistRepository = artistRepository;
        this.artistMapper = artistMapper;
    }

    @Override
    public ArtistResponse findById(Long id) {
        return artistRepository.findById(id)
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + id));
    }

    @Override
    public ArtistResponse findByStageName(String stageName) {
        // Since we don't have findByStageName in standard repo yet, let's use findAll stream or find active
        return artistRepository.findAll().stream()
                .filter(a -> a.getStageName().equalsIgnoreCase(stageName))
                .findFirst()
                .map(artistMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));
    }

    @Override
    public List<ArtistResponse> findActiveArtists() {
        return artistRepository.findAll().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .map(artistMapper::toResponse)
                .toList();
    }
}
