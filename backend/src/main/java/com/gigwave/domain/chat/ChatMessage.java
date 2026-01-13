package com.gigwave.domain.chat;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    private UUID id;

    private UUID threadId;

    private UUID senderId;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    private String text;

    private String mediaUrl;

    private Double locationLat;

    private Double locationLng;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
