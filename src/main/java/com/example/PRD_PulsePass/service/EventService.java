package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.dto.request.CreateEventRequest;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.dto.response.EventSummaryResponse;
import java.util.List;

public interface EventService {
    EventResponse create(CreateEventRequest request);
    EventResponse findByCode(String eventCode);
    List<EventSummaryResponse> findPublishedEvents();
    EventResponse publish(String eventCode);
    EventResponse addArtist(String eventCode, Long artistId);
    List<EventSummaryResponse> findByArtist(String stageName);
}
