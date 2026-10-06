package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.VenueMapper;
import com.example.PRD_PulsePass.repository.VenueRepository;
import com.example.PRD_PulsePass.service.VenueService;
import com.example.PRD_PulsePass.dto.response.VenueResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    @Override
    public VenueResponse findByCode(String code) {
        return venueRepository.findByCode(code)
                .map(venueMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + code));
    }

    @Override
    public List<VenueResponse> findActiveVenues() {
        return venueRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .map(venueMapper::toResponse)
                .toList();
    }
}
