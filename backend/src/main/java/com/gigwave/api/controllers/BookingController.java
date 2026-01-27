package com.gigwave.api.controllers;

import com.gigwave.api.dto.bookings.BookingDto;
import com.gigwave.application.bookings.BookingService;
import com.gigwave.infrastructure.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/gigs/{gigId}/accept")
    @PreAuthorize("hasRole('MUSICIAN')")
    public ResponseEntity<BookingDto> acceptGig(
            @PathVariable UUID gigId,
            @RequestParam BigDecimal acceptedAmount,
            @CurrentUser UUID musicianId
    ) {
        BookingDto booking = bookingService.musicianAcceptsGig(gigId, musicianId, acceptedAmount);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/select")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<BookingDto> selectMusician(
            @PathVariable UUID bookingId,
            @CurrentUser UUID organizerId
    ) {
        BookingDto booking = bookingService.organizerSelectsMusician(bookingId);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/mark-done")
    @PreAuthorize("hasRole('MUSICIAN')")
    public ResponseEntity<BookingDto> markDone(
            @PathVariable UUID bookingId,
            @CurrentUser UUID musicianId
    ) {
        BookingDto booking = bookingService.musicianMarksDone(bookingId, musicianId);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/confirm-pay")
    @PreAuthorize("hasRole('EVENT_OWNER')")
    public ResponseEntity<BookingDto> confirmAndPay(
            @PathVariable UUID bookingId,
            @CurrentUser UUID organizerId
    ) {
        BookingDto booking = bookingService.organizerConfirmAndPay(bookingId, organizerId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable UUID bookingId) {
        BookingDto booking = bookingService.getBookingDetails(bookingId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingDto>> listMyBookings(@CurrentUser UUID userId) {
        List<BookingDto> bookings = bookingService.listBookingsForUser(userId);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{bookingId}/post-gig-media")
    @PreAuthorize("hasRole('MUSICIAN')")
    public ResponseEntity<BookingDto> addPostGigMedia(
            @PathVariable UUID bookingId,
            @RequestBody List<String> mediaUrls,
            @CurrentUser UUID musicianId
    ) {
        BookingDto booking = bookingService.addPostGigMedia(bookingId, musicianId, mediaUrls);
        return ResponseEntity.ok(booking);
    }
}
