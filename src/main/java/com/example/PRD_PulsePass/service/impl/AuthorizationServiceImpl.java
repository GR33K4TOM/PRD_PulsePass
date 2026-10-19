package com.example.PRD_PulsePass.service.impl;

import com.example.PRD_PulsePass.domain.Event;
import com.example.PRD_PulsePass.domain.EventStatus;
import com.example.PRD_PulsePass.domain.Ticket;
import com.example.PRD_PulsePass.domain.User;
import com.example.PRD_PulsePass.repository.UserRepository;
import com.example.PRD_PulsePass.security.SecurityPrincipal;
import com.example.PRD_PulsePass.service.AuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRepository userRepository;

    public AuthorizationServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void checkCanReadUser(User targetUser) {
        Authentication auth = getAuthentication();
        if (auth == null || hasRole(auth, "ROLE_ADMIN")) {
            return;
        }
        if (!isOwner(auth, targetUser)) {
            throw new AccessDeniedException("No tiene permisos para consultar datos de otro usuario");
        }
    }

    @Override
    public void checkCanPurchaseForEmail(String targetEmail) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return;
        }
        User targetUser = userRepository.findByEmailIgnoreCase(targetEmail).orElse(null);
        if (targetUser != null) {
            if (!isOwner(auth, targetUser)) {
                throw new AccessDeniedException("No tiene permisos para comprar entradas a nombre de otro usuario");
            }
        } else {
            String authEmail = extractEmail(auth);
            if (authEmail != null && !authEmail.equalsIgnoreCase(targetEmail)) {
                throw new AccessDeniedException("No tiene permisos para comprar entradas a nombre de otro usuario");
            }
        }
    }

    @Override
    public void checkCanReadTicket(Ticket ticket) {
        Authentication auth = getAuthentication();
        if (auth == null || hasAnyRole(auth, "ROLE_STAFF", "ROLE_ADMIN")) {
            return;
        }
        if (!isOwner(auth, ticket.getUser())) {
            throw new AccessDeniedException("No tiene permisos para consultar una entrada de otro usuario");
        }
    }

    @Override
    public void checkCanReadUserTickets(String targetEmail) {
        Authentication auth = getAuthentication();
        if (auth == null || hasRole(auth, "ROLE_ADMIN")) {
            return;
        }
        User targetUser = userRepository.findByEmailIgnoreCase(targetEmail).orElse(null);
        if (targetUser != null) {
            if (!isOwner(auth, targetUser)) {
                throw new AccessDeniedException("No tiene permisos para consultar las entradas de otro usuario");
            }
        } else {
            String authEmail = extractEmail(auth);
            if (authEmail != null && !authEmail.equalsIgnoreCase(targetEmail)) {
                throw new AccessDeniedException("No tiene permisos para consultar las entradas de otro usuario");
            }
        }
    }

    @Override
    public void checkCanCancelTicket(Ticket ticket) {
        Authentication auth = getAuthentication();
        if (auth == null || hasRole(auth, "ROLE_ADMIN")) {
            return;
        }
        if (!isOwner(auth, ticket.getUser())) {
            throw new AccessDeniedException("No tiene permisos para cancelar una entrada de otro usuario");
        }
    }

    @Override
    public void checkCanViewEvent(Event event) {
        if (event.getStatus() == EventStatus.PUBLISHED) {
            return;
        }
        Authentication auth = getAuthentication();
        if (auth == null) {
            return;
        }
        if (!hasAnyRole(auth, "ROLE_STAFF", "ROLE_ADMIN")) {
            throw new AccessDeniedException("Los eventos no publicados requieren rol STAFF o ADMIN");
        }
    }

    @Override
    public boolean isStaffOrAdmin() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return true;
        }
        return hasAnyRole(auth, "ROLE_STAFF", "ROLE_ADMIN");
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }

    private boolean hasAnyRole(Authentication auth, String... roles) {
        for (String role : roles) {
            if (hasRole(auth, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOwner(Authentication auth, User targetUser) {
        if (auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return false;
        }
        if (targetUser == null) {
            return false;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityPrincipal sp) {
            if (sp.getUserId() != null && targetUser.getId() != null) {
                return sp.getUserId().equals(targetUser.getId());
            }
            if (sp.getEmail() != null && targetUser.getEmail() != null) {
                return sp.getEmail().equalsIgnoreCase(targetUser.getEmail());
            }
            if (sp.getUsername() != null && targetUser.getUsername() != null) {
                return sp.getUsername().equalsIgnoreCase(targetUser.getUsername());
            }
        }
        String name = auth.getName();
        if (name != null) {
            return name.equalsIgnoreCase(targetUser.getUsername()) || name.equalsIgnoreCase(targetUser.getEmail());
        }
        return false;
    }

    private String extractEmail(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityPrincipal sp) {
            return sp.getEmail();
        }
        String name = auth.getName();
        if (name != null && name.contains("@")) {
            return name;
        }
        return null;
    }
}
