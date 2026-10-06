package com.example.PRD_PulsePass.dto.response;

import com.example.PRD_PulsePass.domain.EventCategory;
import com.example.PRD_PulsePass.domain.EventStatus;
import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
    Long id,
    String eventCode,
    String name,
    String description,
    EventCategory category,
    EventStatus status,
    LocalDateTime eventDate,
    Integer minimumAge,
    String venueCode,
    String venueName,
    List<ArtistResponse> artists
) {}
