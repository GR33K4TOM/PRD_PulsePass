package com.example.PRD_PulsePass.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventCode;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private LocalDateTime eventDate;
    private Integer minimumAge;
    
    @Column(name = "streaming_url")
    private String streamingUrl;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToMany
    @JoinTable(
        name = "event_artists",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists;

    @OneToMany(mappedBy = "event")
    private List<Ticket> tickets;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventCategory getCategory() { return category; }
    public void setCategory(EventCategory category) { this.category = category; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
    public Integer getMinimumAge() { return minimumAge; }
    public void setMinimumAge(Integer minimumAge) { this.minimumAge = minimumAge; }
    public String getStreamingUrl() { return streamingUrl; }
    public void setStreamingUrl(String streamingUrl) { this.streamingUrl = streamingUrl; }
    public Venue getVenue() { return venue; }
    public void setVenue(Venue venue) { this.venue = venue; }
    public List<Artist> getArtists() { return artists; }
    public void setArtists(List<Artist> artists) { this.artists = artists; }
    public List<Ticket> getTickets() { return tickets; }
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(eventCode, event.eventCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventCode);
    }
}
