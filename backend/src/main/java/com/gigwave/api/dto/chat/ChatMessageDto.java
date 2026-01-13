package com.gigwave.api.dto.chat;

import com.gigwave.domain.chat.MessageType;
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
public class ChatMessageDto {
    private UUID id;
    private UUID threadId;
    private UUID senderId;
    private MessageType messageType;
    private String text;
    private String mediaUrl;
    private Double locationLat;
    private Double locationLng;
    private LocalDateTime createdAt;
}





