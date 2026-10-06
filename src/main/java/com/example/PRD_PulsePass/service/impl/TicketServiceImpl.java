package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.TicketStatus;
import com.example.PRD_PulsePass.domain.TicketType;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.dto.request.PurchaseTicketRequest;
import com.example.PRD_PulsePass.dto.response.TicketResponse;
import com.example.PRD_PulsePass.exception.BusinessRuleException;
import com.example.PRD_PulsePass.exception.ResourceNotFoundException;
import com.example.PRD_PulsePass.mapper.TicketMapper;
import com.example.PRD_PulsePass.repository.EventRepository;
import com.example.PRD_PulsePass.repository.TicketRepository;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository, UserRepository userRepository,
                             EventRepository eventRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessRuleException("User is not active.");
        }

        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Event is not PUBLISHED.");
        }
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase tickets for past events.");
        }

        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            if (user.getProfile() == null || user.getProfile().getBirthDate() == null) {
                throw new BusinessRuleException("User age unknown.");
            }
            int age = Period.between(user.getProfile().getBirthDate(), event.getEventDate().toLocalDate()).getYears();
            if (age < event.getMinimumAge()) {
                throw new BusinessRuleException("User does not meet minimum age.");
            }
        }

        long paidTickets = ticketRepository.countPaidTicketsByEventCode(event.getEventCode());
        if (paidTickets >= event.getVenue().getCapacity()) {
            throw new BusinessRuleException("Event is SOLD OUT.");
        }

        BigDecimal basePrice = new BigDecimal("100.00");
        BigDecimal price = switch (request.type()) {
            case GENERAL -> basePrice;
            case STUDENT -> basePrice.multiply(new BigDecimal("0.50"));
            case VIP -> basePrice.multiply(new BigDecimal("2.00"));
            case BACKSTAGE -> basePrice.multiply(new BigDecimal("3.00"));
        };

        Ticket ticket = new Ticket();
        ticket.setTicketCode(UUID.randomUUID().toString().substring(0, 8));
        ticket.setType(request.type());
        ticket.setPrice(price);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        Ticket saved = ticketRepository.save(ticket);

        if (paidTickets + 1 == event.getVenue().getCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findAll().stream() // Not ideal for prod, but we didn't add findByTicketCode in repo initially. Wait, findByTicketCode may exist.
                .filter(t -> t.getTicketCode().equals(ticketCode))
                .findFirst()
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCase(email).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findPaidTicketsByEventCode(eventCode).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findAll().stream()
                .filter(t -> t.getTicketCode().equals(ticketCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be cancelled.");
        }
        if (ticket.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel tickets for past events.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findAll().stream()
                .filter(t -> t.getTicketCode().equals(ticketCode))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("CANCELLED ticket cannot be used.");
        }
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Ticket is not PAID.");
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }
}
