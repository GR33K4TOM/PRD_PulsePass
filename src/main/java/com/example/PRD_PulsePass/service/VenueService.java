package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.dto.response.VenueResponse;
import java.util.List;

public interface VenueService {
    VenueResponse findByCode(String code);
    List<VenueResponse> findActiveVenues();
}
