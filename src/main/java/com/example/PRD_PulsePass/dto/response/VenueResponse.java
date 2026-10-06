package com.example.PRD_PulsePass.dto.response;

public record VenueResponse(
    Long id,
    String code,
    String name,
    String city,
    String address,
    Integer capacity,
    Boolean active
) {}
