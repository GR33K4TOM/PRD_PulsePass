package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.TicketStatus;
import com.example.PRD_PulsePass.domain.TicketType;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.domain.UserProfile;
import com.example.PRD_PulsePass.domain.Venue;
import com.example.PRD_PulsePass.dto.request.PurchaseTicketRequest;
import com.example.PRD_PulsePass.dto.response.TicketResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.TicketMapper;
import com.example.PRD_PulsePass.repository.EventRepository;
import com.example.PRD_PulsePass.repository.TicketRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void purchase_validCompra_returnsPAID() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-01", TicketType.GENERAL);
        
        User user = new User();
        user.setActive(true);
        UserProfile profile = new UserProfile();
        profile.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setProfile(profile);

        Venue venue = new Venue();
        venue.setCapacity(100);

        Event event = new Event();
        event.setEventCode("EVT-01");
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMinimumAge(18);
        event.setVenue(venue);

        Ticket saved = new Ticket();
        TicketResponse res = new TicketResponse(1L, "code", TicketType.GENERAL, BigDecimal.TEN, TicketStatus.PAID, LocalDateTime.now(), "test@test.com", "EVT-01", "Name");

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-01")).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode("EVT-01")).thenReturn(50L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);
        when(ticketMapper.toResponse(saved)).thenReturn(res);

        TicketResponse result = ticketService.purchase(req);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
    }

    @Test
    void purchase_userNotFound_throwsNotFound() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("notfound@test.com", "EVT-01", TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase("notfound@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchase_userInactive_throwsBusinessRule() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("inactive@test.com", "EVT-01", TicketType.GENERAL);
        User user = new User();
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("inactive@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchase_eventDraft_throwsBusinessRule() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-DRAFT", TicketType.GENERAL);
        User user = new User();
        user.setActive(true);
        Event event = new Event();
        event.setStatus(EventStatus.DRAFT);
        
        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-DRAFT")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchase_eventCancelled_throwsBusinessRule() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-CANC", TicketType.GENERAL);
        User user = new User();
        user.setActive(true);
        Event event = new Event();
        event.setStatus(EventStatus.CANCELLED);
        
        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-CANC")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchase_underageUser_throwsBusinessRule() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-01", TicketType.GENERAL);
        User user = new User();
        user.setActive(true);
        UserProfile profile = new UserProfile();
        profile.setBirthDate(LocalDate.now().minusYears(15));
        user.setProfile(profile);

        Event event = new Event();
        event.setEventCode("EVT-01");
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMinimumAge(18);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-01")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchase_eventSoldOut_throwsBusinessRule() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-01", TicketType.GENERAL);
        User user = new User();
        user.setActive(true);
        Venue venue = new Venue();
        venue.setCapacity(100);

        Event event = new Event();
        event.setEventCode("EVT-01");
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-01")).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode("EVT-01")).thenReturn(100L);

        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void purchase_lastTicket_updatesEventToSoldOut() {
        PurchaseTicketRequest req = new PurchaseTicketRequest("test@test.com", "EVT-01", TicketType.GENERAL);
        User user = new User();
        user.setActive(true);
        Venue venue = new Venue();
        venue.setCapacity(100);

        Event event = new Event();
        event.setEventCode("EVT-01");
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);

        Ticket saved = new Ticket();
        TicketResponse res = new TicketResponse(1L, "code", TicketType.GENERAL, BigDecimal.TEN, TicketStatus.PAID, LocalDateTime.now(), "test@test.com", "EVT-01", "Name");

        when(userRepository.findByEmailIgnoreCase("test@test.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("EVT-01")).thenReturn(Optional.of(event));
        when(ticketRepository.countPaidTicketsByEventCode("EVT-01")).thenReturn(99L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);
        when(ticketMapper.toResponse(saved)).thenReturn(res);

        ticketService.purchase(req);

        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
    }

    @Test
    void cancel_paidTicket_becomesCancelled() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-01");
        ticket.setStatus(TicketStatus.PAID);
        Event event = new Event();
        event.setEventDate(LocalDateTime.now().plusDays(5));
        ticket.setEvent(event);

        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.cancel("TCK-01");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    void markAsUsed_paidTicket_becomesUsed() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-02");
        ticket.setStatus(TicketStatus.PAID);

        when(ticketRepository.findAll()).thenReturn(List.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        ticketService.markAsUsed("TCK-02");

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-03");
        ticket.setStatus(TicketStatus.CANCELLED);

        when(ticketRepository.findAll()).thenReturn(List.of(ticket));

        assertThatThrownBy(() -> ticketService.markAsUsed("TCK-03"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
