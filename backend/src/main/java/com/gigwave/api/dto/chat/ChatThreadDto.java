package com.gigwave.api.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatThreadDto {
    private UUID id;
    private UUID bookingId;
    private UUID userId1; // For direct messaging
    private UUID userId2; // For direct messaging
    private LocalDateTime createdAt;
}



