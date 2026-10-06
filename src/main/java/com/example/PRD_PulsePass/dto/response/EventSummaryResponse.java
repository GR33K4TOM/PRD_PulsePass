package com.example.PRD_PulsePass.dto.response;

import com.example.PRD_PulsePass.domain.EventStatus;
import java.time.LocalDateTime;

public record EventSummaryResponse(
    Long id,
    String eventCode,
    String name,
    EventStatus status,
    LocalDateTime eventDate,
    String venueCode
) {}
