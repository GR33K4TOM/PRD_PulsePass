package com.example.PRD_PulsePass.dto.response;

import com.example.PRD_PulsePass.domain.TicketStatus;
import com.example.PRD_PulsePass.domain.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
    Long id,
    String ticketCode,
    TicketType type,
    BigDecimal price,
    TicketStatus status,
    LocalDateTime purchaseDate,
    String userEmail,
    String eventCode,
    String eventName
) {}
