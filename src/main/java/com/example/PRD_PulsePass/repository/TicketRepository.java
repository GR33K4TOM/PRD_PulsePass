package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserEmailIgnoreCaseAndStatus(String email, TicketStatus status);
    List<Ticket> findByUserEmailIgnoreCase(String email);

    @Query("SELECT t FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = 'PAID'")
    List<Ticket> findPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = 'PAID'")
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);
}
