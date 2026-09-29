package com.example.PRD_PulsePass.service;

import java.util.List;

import com.example.PRD_PulsePass.service.dto.dto.response.VenueResponse;

public interface VenueService {
    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}
