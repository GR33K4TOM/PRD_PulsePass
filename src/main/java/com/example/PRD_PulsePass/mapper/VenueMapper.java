package com.example.PRD_PulsePass.mapper;

import com.example.PRD_PulsePass.domain.Venue;
import com.example.PRD_PulsePass.dto.response.VenueResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {
    VenueResponse toResponse(Venue venue);
}
