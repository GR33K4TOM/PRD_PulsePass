package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Artist;
import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Venue;
import com.example.PRD_PulsePass.dto.request.CreateEventRequest;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.dto.response.EventSummaryResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.DuplicateResourceException;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.EventMapper;
import com.example.PRD_PulsePass.repository.ArtistRepository;
import com.example.PRD_PulsePass.repository.EventRepository;
import com.example.PRD_PulsePass.repository.VenueRepository;
import com.example.PRD_PulsePass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, VenueRepository venueRepository,
                            ArtistRepository artistRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        if (eventRepository.findByEventCode(request.eventCode()).isPresent()) {
            throw new DuplicateResourceException("Event code already exists.");
        }
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));
        
        if (!Boolean.TRUE.equals(venue.getActive())) {
            throw new BusinessRuleException("Cannot create event in inactive venue.");
        }
        if (request.eventDate() != null && request.eventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }
        if (request.minimumAge() != null && request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative.");
        }

        Event event = new Event();
        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge() == null ? 0 : request.minimumAge());
        event.setVenue(venue);
        event.setArtists(new ArrayList<>());

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED).stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published.");
        }
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish past events.");
        }
        if (!Boolean.TRUE.equals(event.getVenue().getActive())) {
            throw new BusinessRuleException("Venue must be active.");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));
        
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot add artist to CANCELLED or FINISHED event.");
        }
        
        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException("Artist already added to event.");
        }
        
        event.getArtists().add(artist);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName).stream()
                .map(eventMapper::toSummary)
                .toList();
    }
}
