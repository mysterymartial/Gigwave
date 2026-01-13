package com.gigwave.domain.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "chat_threads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatThread {
    @Id
    private UUID id;

    // For booking-based chat
    @Indexed(unique = true, sparse = true)
    private UUID bookingId;

    // For direct messaging between users
    @Indexed(sparse = true)
    private UUID userId1;
    
    @Indexed(sparse = true)
    private UUID userId2;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
