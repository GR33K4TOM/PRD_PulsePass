package com.example.PRD_PulsePass.service.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.example.PRD_PulsePass.domain.EventCategory;
import com.example.PRD_PulsePass.domain.EventStatus;

@Transactional (readOnly = true)
public record EventResponse(
    Long id,
    String eventCode,
    String name,
    String description,
    EventCategory category,
    EventStatus status,
    LocalDateTime eventDate,
    Integer minimunAge,
    Long venueCode,
    String venueName,
    List<String> artists
) {}
