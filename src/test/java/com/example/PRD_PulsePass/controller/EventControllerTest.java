package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.domain.EventCategory;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.dto.request.CreateEventRequest;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.dto.response.EventSummaryResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void testCtrlEvt001_crearValido_returns201() throws Exception {
        LocalDateTime date = LocalDateTime.now().plusDays(30);
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.DRAFT, date, 18,
                "VEN-SMR-01", "Marina Convention Center", List.of()
        );
        when(eventService.create(any(CreateEventRequest.class))).thenReturn(response);

        String json = """
                {
                    "eventCode": "CMF-2026",
                    "name": "Caribbean Music Fest 2026",
                    "description": "Concierto anual",
                    "category": "MUSIC",
                    "eventDate": "2026-11-20T20:00:00",
                    "minimumAge": 18,
                    "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    void testCtrlEvt002_requestInvalido_returns400() throws Exception {
        String invalidJson = """
                {
                    "eventCode": "",
                    "name": "",
                    "category": null,
                    "minimumAge": -1
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isMap());

        verify(eventService, never()).create(any());
    }

    @Test
    void testCtrlEvt003_consultarExistente_returns200() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of()
        );
        when(eventService.findByCode("CMF-2026")).thenReturn(response);

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    void testCtrlEvt004_inexistente_returns404() throws Exception {
        when(eventService.findByCode("NOTFOUND")).thenThrow(new ResourceNotFoundException("Event not found: NOTFOUND"));

        mockMvc.perform(get("/api/events/{eventCode}", "NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(eventService).findByCode("NOTFOUND");
    }

    @Test
    void testCtrlEvt005_listarPublicados_returns200() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30),
                "VEN-SMR-01"
        );
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findPublishedEvents();
    }

    @Test
    void testCtrlEvt006_publicarValido_returns200() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of()
        );
        when(eventService.publish("CMF-2026")).thenReturn(response);

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    void testCtrlEvt007_publicarInvalido_returns409() throws Exception {
        when(eventService.publish("CMF-2026")).thenThrow(new BusinessRuleException("Only DRAFT events can be published."));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published."));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    void testCtrlEvt008_asociarArtista_returns200() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of()
        );
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(response);

        mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    void testCtrlEvt009_buscarPorArtista_returns200() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30),
                "VEN-SMR-01"
        );
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }
}
