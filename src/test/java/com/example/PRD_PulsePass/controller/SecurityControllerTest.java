package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.domain.EventCategory;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.TicketStatus;
import com.example.PRD_PulsePass.domain.TicketType;
import com.example.PRD_PulsePass.dto.request.CreateEventRequest;
import com.example.PRD_PulsePass.dto.request.LoginRequest;
import com.example.PRD_PulsePass.dto.request.PurchaseTicketRequest;
import com.example.PRD_PulsePass.dto.response.AuthResponse;
import com.example.PRD_PulsePass.dto.response.AuthenticatedUserResponse;
import com.example.PRD_PulsePass.dto.response.EventResponse;
import com.example.PRD_PulsePass.dto.response.EventSummaryResponse;
import com.example.PRD_PulsePass.dto.response.TicketResponse;
import com.example.PRD_PulsePass.exception.GlobalExceptionHandler;
import com.example.PRD_PulsePass.security.CustomUserDetailsService;
import com.example.PRD_PulsePass.security.JwtAuthenticationFilter;
import com.example.PRD_PulsePass.security.JwtProperties;
import com.example.PRD_PulsePass.security.JwtService;
import com.example.PRD_PulsePass.security.RestAccessDeniedHandler;
import com.example.PRD_PulsePass.security.RestAuthenticationEntryPoint;
import com.example.PRD_PulsePass.security.SecurityConfig;
import com.example.PRD_PulsePass.security.SecurityPrincipal;
import com.example.PRD_PulsePass.service.ArtistService;
import com.example.PRD_PulsePass.service.AuthService;
import com.example.PRD_PulsePass.service.EventService;
import com.example.PRD_PulsePass.service.TicketService;
import com.example.PRD_PulsePass.service.UserService;
import com.example.PRD_PulsePass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        AuthController.class,
        EventController.class,
        TicketController.class,
        UserController.class,
        VenueController.class,
        ArtistController.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void tSec001_anonymousGetPublishedEvents_returns200() throws Exception {
        EventSummaryResponse summary = new EventSummaryResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), "VEN-SMR-01"
        );
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));
    }

    @Test
    void tSec002_anonymousPostEvents_returns401() throws Exception {
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(eventService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void tSec003_userPostEvents_returns403WithoutInvokingService() throws Exception {
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(eventService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tSec004_adminPostEvents_returns201() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusDays(30), 18,
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
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    void tSec005_anonymousLoginValid_returns200WithBearer() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "Bearer", 900L));

        String json = """
                {
                    "identifier": "andrea",
                    "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void tSec006_loginWrongPassword_returns401Generic() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        String json = """
                {
                    "identifier": "andrea",
                    "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void tSec007_expiredTokenOnProtectedRoute_returns401WithErrorResponse() throws Exception {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setIssuer("pulsepass-api");
        expiredProps.setSecretBase64("dGVzdC1vbmx5LW5vdC1mb3ItcHJvZHVjdGlvbi1zZWNyZXQta2V5LTIwMjYtMzJieXRlcw==");
        expiredProps.setAccessTokenTtl(Duration.ofSeconds(-5));
        JwtService expiredJwtService = new JwtService(expiredProps);
        expiredJwtService.init();

        String expiredToken = expiredJwtService.generateToken(1L);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(authService, never()).getCurrentUser();
    }

    @Test
    void tSec008_tamperedTokenSignature_returns401WithoutInvokingService() throws Exception {
        String validToken = jwtService.generateToken(1L);
        String tamperedToken = validToken.substring(0, validToken.length() - 2) + (validToken.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/api/tickets/TCK-0001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(ticketService, never()).findByCode(any());
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void tSec009_userQueriesForeignTicket_returns403() throws Exception {
        when(ticketService.findByCode("TCK-CARLOS"))
                .thenThrow(new AccessDeniedException("No tiene permisos para consultar una entrada de otro usuario"));

        mockMvc.perform(get("/api/tickets/TCK-CARLOS"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void tSec010_userPurchasesWithForeignEmail_returns403() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new AccessDeniedException("No tiene permisos para comprar entradas a nombre de otro usuario"));

        String json = """
                {
                    "userEmail": "carlos@email.com",
                    "eventCode": "CMF-2026",
                    "type": "VIP"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void tSec011_userCancelsOwnTicket_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.CANCELLED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.cancel("TCK-0001")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void tSec012_userAttemptsToUseTicket_returns403() throws Exception {
        mockMvc.perform(patch("/api/tickets/TCK-0001/use"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(ticketService, never()).markAsUsed(any());
    }

    @Test
    @WithMockUser(username = "laura", roles = "STAFF")
    void tSec013_staffMarksTicketAsUsed_returns200() throws Exception {
        TicketResponse response = new TicketResponse(
                1L, "TCK-0001", TicketType.VIP, new BigDecimal("250000.00"),
                TicketStatus.USED, LocalDateTime.now(), "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketService.markAsUsed("TCK-0001")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/TCK-0001/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-0001");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void tSec014_adminPublishesEvent_returns200() throws Exception {
        EventResponse response = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026", "Concierto anual",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of()
        );
        when(eventService.publish("CMF-2026")).thenReturn(response);

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(username = "andrea", roles = "USER")
    void tSec015_userQueriesAnotherUserData_returns403() throws Exception {
        when(userService.findByEmail("carlos@email.com"))
                .thenThrow(new AccessDeniedException("No tiene permisos para consultar datos de otro usuario"));

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@email.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void tSec016_validTokenBelongingToInactiveUser_returns401() throws Exception {
        String token = jwtService.generateToken(2L);
        SecurityPrincipal disabledPrincipal = new SecurityPrincipal(
                2L, "miguel", "miguel@email.com", "hash", false,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(customUserDetailsService.loadUserById(2L)).thenReturn(disabledPrincipal);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tSec017_anonymousQueriesUnpublishedEvent_doesNotExposeDraft() throws Exception {
        when(eventService.findByCode("CMF-2026-DRAFT"))
                .thenThrow(new AccessDeniedException("Los eventos no publicados requieren rol STAFF o ADMIN"));

        mockMvc.perform(get("/api/events/CMF-2026-DRAFT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tSec018_invalidRequestButAuthorized_returns400BeanValidation() throws Exception {
        String invalidJson = """
                {
                    "eventCode": "",
                    "name": ""
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(eventService, never()).create(any());
    }

    @Test
    void tSec019_loginOrJwtError_doesNotLeakStacktracePasswordOrHash() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        String json = """
                {
                    "identifier": "andrea",
                    "password": "SecretPasswordToTestLeak!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("SecretPasswordToTestLeak!"))))
                .andExpect(content().string(not(containsString("$2a$"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void tSec020_authMeWithValidJwt_returnsAuthenticatedUserResponse() throws Exception {
        String token = jwtService.generateToken(1L);
        SecurityPrincipal principal = new SecurityPrincipal(
                1L, "andrea", "andrea@email.com", "$2a$10$hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        when(customUserDetailsService.loadUserById(1L)).thenReturn(principal);
        when(authService.getCurrentUser()).thenReturn(
                new AuthenticatedUserResponse(1L, "andrea", "andrea@email.com", List.of("ROLE_USER"))
        );

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }
}
