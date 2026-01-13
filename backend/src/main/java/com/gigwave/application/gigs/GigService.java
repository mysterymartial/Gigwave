package com.gigwave.application.gigs;

import com.gigwave.api.dto.gigs.GigDto;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.gigs.GigStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GigService {
    private final GigRepository gigRepository;

    @Transactional
    public GigDto createGig(GigDto dto) {
        Gig gig = Gig.builder()
                .organizerId(dto.getOrganizerId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(dto.getLocation())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .venuePictureUrl(dto.getVenuePictureUrl())
                .budgetMin(dto.getBudgetMin())
                .budgetMax(dto.getBudgetMax())
                .status(GigStatus.OPEN)
                .build();

        gig = gigRepository.save(gig);
        return toDto(gig);
    }

    @Transactional
    public GigDto updateGig(UUID gigId, GigDto dto) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));

        gig.setTitle(dto.getTitle());
        gig.setDescription(dto.getDescription());
        gig.setEventDate(dto.getEventDate());
        gig.setLocation(dto.getLocation());
        gig.setLatitude(dto.getLatitude());
        gig.setLongitude(dto.getLongitude());
        gig.setVenuePictureUrl(dto.getVenuePictureUrl());
        gig.setBudgetMin(dto.getBudgetMin());
        gig.setBudgetMax(dto.getBudgetMax());

        gig = gigRepository.save(gig);
        return toDto(gig);
    }

    @Transactional
    public void cancelGig(UUID gigId) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
        gig.setStatus(GigStatus.CANCELLED);
        gigRepository.save(gig);
    }

    public List<GigDto> listGigsForOrganizer(UUID organizerId) {
        return gigRepository.findByOrganizerId(organizerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<GigDto> listOpenGigsForMusician(String city, BigDecimal minBudget, BigDecimal maxBudget) {
        List<Gig> allOpenGigs = gigRepository.findByStatusAndEventDateAfter(
                GigStatus.OPEN,
                LocalDateTime.now()
        );
        
        return allOpenGigs.stream()
                .filter(gig -> city == null || (gig.getLocation() != null && 
                    gig.getLocation().toLowerCase().contains(city.toLowerCase())))
                .filter(gig -> minBudget == null || (gig.getBudgetMax() != null && 
                    gig.getBudgetMax().compareTo(minBudget) >= 0))
                .filter(gig -> maxBudget == null || (gig.getBudgetMin() != null && 
                    gig.getBudgetMin().compareTo(maxBudget) <= 0))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public GigDto getGigById(UUID gigId) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
        return toDto(gig);
    }

    private GigDto toDto(Gig gig) {
        return GigDto.builder()
                .id(gig.getId())
                .organizerId(gig.getOrganizerId())
                .title(gig.getTitle())
                .description(gig.getDescription())
                .eventDate(gig.getEventDate())
                .location(gig.getLocation())
                .latitude(gig.getLatitude())
                .longitude(gig.getLongitude())
                .venuePictureUrl(gig.getVenuePictureUrl())
                .budgetMin(gig.getBudgetMin())
                .budgetMax(gig.getBudgetMax())
                .status(gig.getStatus())
                .createdAt(gig.getCreatedAt())
                .build();
    }
}

