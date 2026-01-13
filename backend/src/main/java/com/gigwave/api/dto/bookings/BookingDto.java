package com.gigwave.api.dto.bookings;

import com.gigwave.domain.bookings.BookingStatus;
import com.gigwave.domain.bookings.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private UUID id;
    private UUID gigId;
    private UUID musicianId;
    private UUID organizerMandateId;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal acceptedAmount;
    private LocalDateTime acceptedAt;
    private LocalDateTime musicianDoneAt;
    private LocalDateTime ownerConfirmedAt;
    private LocalDateTime completedAt;
    private List<String> postGigMediaUrls;
    private LocalDateTime createdAt;
}


