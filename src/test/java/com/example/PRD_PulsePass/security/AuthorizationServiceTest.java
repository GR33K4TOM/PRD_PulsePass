package com.example.PRD_PulsePass.security;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setPrincipal(Long id, String username, String email, String role) {
        SecurityPrincipal principal = new SecurityPrincipal(
                id, username, email, "hash", true,
                List.of(new SimpleGrantedAuthority(role))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void checkCanReadTicket_ownerVsThirdParty() {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("andrea");
        owner.setEmail("andrea@email.com");

        Ticket ticket = new Ticket();
        ticket.setUser(owner);

        setPrincipal(1L, "andrea", "andrea@email.com", "ROLE_USER");
        assertThatCode(() -> authorizationService.checkCanReadTicket(ticket)).doesNotThrowAnyException();

        setPrincipal(2L, "carlos", "carlos@email.com", "ROLE_USER");
        assertThatThrownBy(() -> authorizationService.checkCanReadTicket(ticket))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkCanReadTicket_staffAndAdminAllowed() {
        User owner = new User();
        owner.setId(1L);
        Ticket ticket = new Ticket();
        ticket.setUser(owner);

        setPrincipal(3L, "laura", "laura@email.com", "ROLE_STAFF");
        assertThatCode(() -> authorizationService.checkCanReadTicket(ticket)).doesNotThrowAnyException();

        setPrincipal(4L, "admin", "admin@email.com", "ROLE_ADMIN");
        assertThatCode(() -> authorizationService.checkCanReadTicket(ticket)).doesNotThrowAnyException();
    }

    @Test
    void checkCanPurchaseForEmail_rejectsForeignEmail() {
        User carlos = new User();
        carlos.setId(2L);
        carlos.setUsername("carlos");
        carlos.setEmail("carlos@email.com");

        when(userRepository.findByEmailIgnoreCase("carlos@email.com")).thenReturn(Optional.of(carlos));

        setPrincipal(1L, "andrea", "andrea@email.com", "ROLE_USER");
        assertThatThrownBy(() -> authorizationService.checkCanPurchaseForEmail("carlos@email.com"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkCanViewEvent_draftRequiresStaffOrAdmin() {
        Event draft = new Event();
        draft.setStatus(EventStatus.DRAFT);

        setPrincipal(1L, "andrea", "andrea@email.com", "ROLE_USER");
        assertThatThrownBy(() -> authorizationService.checkCanViewEvent(draft))
                .isInstanceOf(AccessDeniedException.class);

        setPrincipal(4L, "admin", "admin@email.com", "ROLE_ADMIN");
        assertThatCode(() -> authorizationService.checkCanViewEvent(draft)).doesNotThrowAnyException();
    }
}
