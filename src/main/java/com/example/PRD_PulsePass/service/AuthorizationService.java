package com.example.PRD_PulsePass.service;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.User;

public interface AuthorizationService {
    void checkCanReadUser(User targetUser);
    void checkCanPurchaseForEmail(String targetEmail);
    void checkCanReadTicket(Ticket ticket);
    void checkCanReadUserTickets(String targetEmail);
    void checkCanCancelTicket(Ticket ticket);
    void checkCanViewEvent(Event event);
    boolean isStaffOrAdmin();
}
