package com.gigwave.domain.bookings;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    private UUID id;

    private UUID gigId;

    private UUID musicianId;

    private UUID organizerMandateId;

    @Builder.Default
    private BookingStatus bookingStatus = BookingStatus.REQUESTED;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.NOT_INITIATED;

    private BigDecimal acceptedAmount;

    private LocalDateTime acceptedAt;

    private LocalDateTime musicianDoneAt;

    private LocalDateTime ownerConfirmedAt;

    private LocalDateTime completedAt;

    // Post-gig media (video or picture) uploaded by musician
    @Builder.Default
    private List<String> postGigMediaUrls = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
