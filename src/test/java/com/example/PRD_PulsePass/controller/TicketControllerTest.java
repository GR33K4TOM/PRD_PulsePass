package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.domain.TicketStatus;
import com.example.PRD_PulsePass.domain.TicketType;
import com.example.PRD_PulsePass.dto.request.PurchaseTicketRequest;
import com.example.PRD_PulsePass.dto.response.TicketResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.security.CustomUserDetailsService;
import com.example.PRD_PulsePass.security.JwtAuthenticationFilter;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.security.RestAccessDeniedHandler;
import com.example.PRD_PulsePass.security.RestAuthenticationEntryPoint;
import com.example.PRD_PulsePass.security.SecurityConfig;
import com.example.PRD_PulsePass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt001_compraValida_returns201() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(response);

        String json = """
                {
                    "userEmail": "andrea@email.com",
                    "eventCode": "CMF-2026",
                    "type": "VIP"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt002_requestInvalido_returns400() throws Exception {
        String invalidJson = """
                {
                    "userEmail": "",
                    "eventCode": "",
                    "type": null
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt003_usuarioInexistente_returns404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: notfound@email.com"));

        String json = """
                {
                    "userEmail": "notfound@email.com",
                    "eventCode": "CMF-2026",
                    "type": "VIP"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: notfound@email.com"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @WithMockUser(username = "laura", roles = "USER")
    void testCtrlTkt004_reglaDeNegocio_returns409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet minimum age."));

        String json = """
                {
                    "userEmail": "laura@email.com",
                    "eventCode": "CMF-2026",
                    "type": "GENERAL"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet minimum age."));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt005_consultarTicket_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.findByCode("TCK-0001")).thenReturn(response);

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-0001"));

        verify(ticketService).findByCode("TCK-0001");
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt006_ticketsPorUsuario_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.findByUserEmail("andrea@email.com")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-0001"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testCtrlTkt007_ticketsPaidPorEvento_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.PAID, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-0001"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt008_cancelarValido_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.CANCELLED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.cancel("TCK-0001")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-0001");
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void testCtrlTkt009_cancelarInvalido_returns409() throws Exception {
        when(ticketService.cancel("TCK-0001"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled."));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", "TCK-0001"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only PAID tickets can be cancelled."));

        verify(ticketService).cancel("TCK-0001");
    }

    @Test
    @WithMockUser(username = "laura", roles = "STAFF")
    void testCtrlTkt010_usarValido_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.USED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.markAsUsed("TCK-0001")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-0001");
    }

    @Test
    @WithMockUser(username = "laura", roles = "STAFF")
    void testCtrlTkt011_usarInvalido_returns409() throws Exception {
        when(ticketService.markAsUsed("TCK-0001"))
                .thenThrow(new BusinessRuleException("CANCELLED ticket cannot be used."));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use", "TCK-0001"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("CANCELLED ticket cannot be used."));

        verify(ticketService).markAsUsed("TCK-0001");
    }
}
