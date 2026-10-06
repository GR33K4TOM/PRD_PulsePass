package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventCategory;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Venue;
import com.example.PRD_PulsePass.dto.request.CreateEventRequest;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.EventMapper;
import com.example.PRD_PulsePass.repository.ArtistRepository;
import com.example.PRD_PulsePass.repository.EventRepository;
import com.example.PRD_PulsePass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void findByCode_eventExists_returnsDto() {
        Event event = new Event();
        event.setEventCode("EVT-01");
        EventResponse response = new EventResponse(1L, "EVT-01", "Name", null, null, null, null, null, null, null, null);
        
        when(eventRepository.findByEventCode("EVT-01")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        EventResponse result = eventService.findByCode("EVT-01");
        
        assertThat(result).isNotNull();
        assertThat(result.eventCode()).isEqualTo("EVT-01");
    }

    @Test
    void findByCode_eventNotExists_throwsResourceNotFound() {
        when(eventRepository.findByEventCode("EVT-02")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByCode("EVT-02"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_validEvent_savesAndReturns() {
        CreateEventRequest req = new CreateEventRequest("EVT-03", "Test", "Desc", EventCategory.MUSIC, LocalDateTime.now().plusDays(5), 18, "VEN-01");
        Venue venue = new Venue();
        venue.setCode("VEN-01");
        venue.setActive(true);
        Event saved = new Event();
        EventResponse response = new EventResponse(1L, "EVT-03", "Name", null, null, null, null, null, null, null, null);

        when(eventRepository.findByEventCode("EVT-03")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-01")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(eventMapper.toResponse(saved)).thenReturn(response);

        EventResponse result = eventService.create(req);
        
        assertThat(result).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_venueNotExists_throwsAndDoesNotSave() {
        CreateEventRequest req = new CreateEventRequest("EVT-04", "Test", "Desc", EventCategory.MUSIC, LocalDateTime.now().plusDays(5), 18, "VEN-02");
        when(eventRepository.findByEventCode("EVT-04")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-02")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_venueInactive_throwsBusinessRule() {
        CreateEventRequest req = new CreateEventRequest("EVT-05", "Test", "Desc", EventCategory.MUSIC, LocalDateTime.now().plusDays(5), 18, "VEN-03");
        Venue venue = new Venue();
        venue.setCode("VEN-03");
        venue.setActive(false);
        
        when(eventRepository.findByEventCode("EVT-05")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-03")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> eventService.create(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_pastDate_throwsBusinessRule() {
        CreateEventRequest req = new CreateEventRequest("EVT-06", "Test", "Desc", EventCategory.MUSIC, LocalDateTime.now().minusDays(5), 18, "VEN-04");
        Venue venue = new Venue();
        venue.setCode("VEN-04");
        venue.setActive(true);
        
        when(eventRepository.findByEventCode("EVT-06")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-04")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> eventService.create(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void publish_draftEvent_becomesPublished() {
        Event event = new Event();
        event.setStatus(EventStatus.DRAFT);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        Venue venue = new Venue();
        venue.setActive(true);
        event.setVenue(venue);
        
        when(eventRepository.findByEventCode("EVT-07")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        
        eventService.publish("EVT-07");
        
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void publish_cancelledEvent_throwsAndDoesNotSave() {
        Event event = new Event();
        event.setStatus(EventStatus.CANCELLED);
        
        when(eventRepository.findByEventCode("EVT-08")).thenReturn(Optional.of(event));
        
        assertThatThrownBy(() -> eventService.publish("EVT-08"))
                .isInstanceOf(BusinessRuleException.class);
        
        verify(eventRepository, never()).save(event);
    }
}
