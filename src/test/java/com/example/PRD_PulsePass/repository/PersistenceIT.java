package com.example.PRD_PulsePass.repository;

import com.example.PRD_PulsePass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class PersistenceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void testVenuePersistence() {
        Venue venue = new Venue();
        venue.setCode("VEN-01");
        venue.setName("Main Hall");
        venue.setCity("Test City");
        venue.setAddress("123 Test St");
        venue.setCapacity(500);
        venue.setActive(true);

        Venue saved = venueRepository.save(venue);
        assertThat(saved.getId()).isNotNull();

        Venue found = venueRepository.findByCode("VEN-01").orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCapacity()).isEqualTo(500);
    }
}
