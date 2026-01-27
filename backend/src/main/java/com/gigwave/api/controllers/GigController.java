package com.gigwave.api.controllers;

import com.gigwave.api.dto.gigs.GigDto;
import com.gigwave.application.gigs.GigService;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gigs")
@RequiredArgsConstructor
public class GigController {
    private final GigService gigService;

    @PostMapping
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<GigDto> createGig(@Valid @RequestBody GigDto dto, @CurrentUser UUID userId) {
        dto.setOrganizerId(userId);
        GigDto created = gigService.createGig(dto);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{gigId}")
    public ResponseEntity<GigDto> getGig(@PathVariable UUID gigId) {
        GigDto gig = gigService.getGigById(gigId);
        return ResponseEntity.ok(gig);
    }

    @GetMapping
    public ResponseEntity<List<GigDto>> listOpenGigs(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget
    ) {
        List<GigDto> gigs = gigService.listOpenGigsForMusician(city, minBudget, maxBudget);
        return ResponseEntity.ok(gigs);
    }

    @GetMapping("/organizer/my-gigs")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<List<GigDto>> listMyGigs(@CurrentUser UUID userId) {
        List<GigDto> gigs = gigService.listGigsForOrganizer(userId);
        return ResponseEntity.ok(gigs);
    }

    @PutMapping("/{gigId}")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<GigDto> updateGig(
            @PathVariable UUID gigId,
            @Valid @RequestBody GigDto dto,
            @CurrentUser UUID userId
    ) {
        // Verify ownership
        GigDto existing = gigService.getGigById(gigId);
        if (!existing.getOrganizerId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        GigDto updated = gigService.updateGig(gigId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{gigId}")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<Void> cancelGig(@PathVariable UUID gigId, @CurrentUser UUID userId) {
        GigDto existing = gigService.getGigById(gigId);
        if (!existing.getOrganizerId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        gigService.cancelGig(gigId);
        return ResponseEntity.noContent().build();
    }
}
