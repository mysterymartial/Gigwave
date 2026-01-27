package com.gigwave.api.controllers;

import com.gigwave.api.dto.disputes.DisputeDto;
import com.gigwave.application.disputes.DisputeService;
import com.gigwave.domain.disputes.Dispute;
import com.gigwave.domain.disputes.DisputeStatus;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {
    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<DisputeDto> openDispute(
            @Valid @RequestBody DisputeDto dto,
            @CurrentUser UUID raisedBy
    ) {
        Dispute dispute = disputeService.openDispute(
                dto.getBookingId(),
                raisedBy,
                dto.getReason(),
                dto.getEvidenceUrl()
        );
        return ResponseEntity.ok(toDto(dispute));
    }

    @PostMapping("/{disputeId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeDto> resolveDispute(
            @PathVariable UUID disputeId,
            @RequestParam DisputeStatus resolution,
            @CurrentUser UUID resolvedBy
    ) {
        Dispute dispute = disputeService.resolveDispute(disputeId, resolution, resolvedBy);
        return ResponseEntity.ok(toDto(dispute));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<List<DisputeDto>> listDisputesForBooking(@PathVariable UUID bookingId) {
        List<Dispute> disputes = disputeService.listDisputesForBooking(bookingId);
        return ResponseEntity.ok(disputes.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/my-disputes")
    public ResponseEntity<List<DisputeDto>> listMyDisputes(@CurrentUser UUID userId) {
        List<Dispute> disputes = disputeService.listDisputesForUser(userId);
        return ResponseEntity.ok(disputes.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<DisputeDto> getDispute(@PathVariable UUID disputeId) {
        Dispute dispute = disputeService.getDispute(disputeId);
        return ResponseEntity.ok(toDto(dispute));
    }

    private DisputeDto toDto(Dispute dispute) {
        return DisputeDto.builder()
                .id(dispute.getId())
                .bookingId(dispute.getBookingId())
                .raisedBy(dispute.getRaisedBy())
                .reason(dispute.getReason())
                .evidenceUrl(dispute.getEvidenceUrl())
                .status(dispute.getStatus())
                .createdAt(dispute.getCreatedAt())
                .resolvedAt(dispute.getResolvedAt())
                .build();
    }
}
