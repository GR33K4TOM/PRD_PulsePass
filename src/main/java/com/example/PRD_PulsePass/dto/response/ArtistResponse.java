package com.example.PRD_PulsePass.dto.response;

public record ArtistResponse(
    Long id,
    String stageName,
    String country,
    String genre,
    Boolean active
) {}
