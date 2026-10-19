package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.dto.response.ArtistResponse;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.security.CustomUserDetailsService;
import com.example.PRD_PulsePass.security.JwtAuthenticationFilter;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.security.RestAccessDeniedHandler;
import com.example.PRD_PulsePass.security.RestAuthenticationEntryPoint;
import com.example.PRD_PulsePass.security.SecurityConfig;
import com.example.PRD_PulsePass.service.ArtistService;
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

@WebMvcTest(ArtistController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void testCtrlArt001_buscarPorId_returns200() throws Exception {
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
        when(artistService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    void testCtrlArt002_idInexistente_returns404() throws Exception {
        when(artistService.findById(999L)).thenThrow(new ResourceNotFoundException("Artist not found: 999"));

        mockMvc.perform(get("/api/artists/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(artistService).findById(999L);
    }

    @Test
    void testCtrlArt003_buscarPorStageName_returns200() throws Exception {
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
        when(artistService.findByStageName("Solar Beat")).thenReturn(response);

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void testCtrlArt004_listarActivos_returns200() throws Exception {
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
        when(artistService.findActiveArtists()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
    }
}
