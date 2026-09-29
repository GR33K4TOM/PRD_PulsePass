package com.example.PRD_PulsePass.service.dto.request;

import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

import com.example.PRD_PulsePass.domain.EventCategory;

@Transactional(readOnly = true)
public record CreateEventRequest( 

    String eventCode,
    String name,
    String description,
    EventCategory category,
    LocalDateTime minimumAge,
    String venueCode
)
{}
