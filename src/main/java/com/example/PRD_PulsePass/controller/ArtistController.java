package com.example.PRD_PulsePass.controller;

import com.example.PRD_PulsePass.dto.response.ArtistResponse;
import com.example.PRD_PulsePass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.findById(id));
    }

    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> getByStageName(@RequestParam String stageName) {
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> getActive() {
        return ResponseEntity.ok(artistService.findActiveArtists());
    }
}
