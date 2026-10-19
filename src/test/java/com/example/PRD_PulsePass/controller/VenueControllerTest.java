package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.dto.response.VenueResponse;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @Test
    void testCtrlVen001_venueExistente_returns200() throws Exception {
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Dir 1", 5000, true);
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(response);

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test
    void testCtrlVen002_venueInexistente_returns404() throws Exception {
        when(venueService.findByCode("INVALID")).thenThrow(new ResourceNotFoundException("Venue not found: INVALID"));

        mockMvc.perform(get("/api/venues/{code}", "INVALID"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: INVALID"));

        verify(venueService).findByCode("INVALID");
    }

    @Test
    void testCtrlVen003_venuesActivos_returns200() throws Exception {
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Dir 1", 5000, true);
        when(venueService.findActiveVenues()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"));

        verify(venueService).findActiveVenues();
    }
}
