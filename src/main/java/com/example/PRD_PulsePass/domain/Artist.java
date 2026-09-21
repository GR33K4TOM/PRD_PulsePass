package com.example.PRD_PulsePass.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "artists")
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String stageName;

    private String country;
    private String genre;
    private Boolean active;

    @ManyToMany(mappedBy = "artists")
    private List<Event> events;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return Objects.equals(stageName, artist.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageName);
    }
}
