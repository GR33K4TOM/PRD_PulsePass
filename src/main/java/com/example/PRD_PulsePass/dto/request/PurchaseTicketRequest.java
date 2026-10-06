package com.example.PRD_PulsePass.dto.request;

import com.example.PRD_PulsePass.domain.TicketType;

public record PurchaseTicketRequest(
    String userEmail,
    String eventCode,
    TicketType type
) {}
