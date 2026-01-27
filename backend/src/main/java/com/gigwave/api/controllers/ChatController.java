package com.gigwave.api.controllers;

import com.gigwave.api.dto.chat.ChatMessageDto;
import com.gigwave.api.dto.chat.ChatThreadDto;
import com.gigwave.application.chat.ChatService;
import com.gigwave.domain.chat.ChatMessage;
import com.gigwave.domain.chat.ChatThread;
import com.gigwave.domain.chat.MessageType;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/bookings/{bookingId}/thread")
    public ResponseEntity<ChatThreadDto> openThreadForBooking(@PathVariable UUID bookingId) {
        ChatThread thread = chatService.openThreadForBooking(bookingId);
        return ResponseEntity.ok(toThreadDto(thread));
    }

    @GetMapping("/bookings/{bookingId}/thread")
    public ResponseEntity<ChatThreadDto> getThreadByBookingId(@PathVariable UUID bookingId) {
        ChatThread thread = chatService.getThreadByBookingId(bookingId);
        return ResponseEntity.ok(toThreadDto(thread));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable UUID threadId,
            @Valid @RequestBody ChatMessageDto dto,
            @CurrentUser UUID senderId
    ) {
        ChatMessage message = chatService.sendMessage(
                threadId,
                senderId,
                dto.getMessageType(),
                dto.getText(),
                dto.getMediaUrl(),
                dto.getLocationLat(),
                dto.getLocationLng()
        );
        return ResponseEntity.ok(toMessageDto(message));
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatMessageDto>> listMessages(@PathVariable UUID threadId) {
        List<ChatMessage> messages = chatService.listMessages(threadId);
        return ResponseEntity.ok(messages.stream().map(this::toMessageDto).collect(Collectors.toList()));
    }

    @PostMapping("/direct/{userId1}/{userId2}")
    public ResponseEntity<ChatThreadDto> openDirectThread(
            @PathVariable UUID userId1,
            @PathVariable UUID userId2,
            @CurrentUser UUID currentUserId
    ) {
        // Verify current user is one of the participants
        if (!currentUserId.equals(userId1) && !currentUserId.equals(userId2)) {
            throw new IllegalArgumentException("You can only create direct threads involving yourself");
        }
        ChatThread thread = chatService.openDirectThread(userId1, userId2);
        return ResponseEntity.ok(toThreadDto(thread));
    }

    @GetMapping("/direct/{userId1}/{userId2}")
    public ResponseEntity<ChatThreadDto> getDirectThread(
            @PathVariable UUID userId1,
            @PathVariable UUID userId2,
            @CurrentUser UUID currentUserId
    ) {
        // Verify current user is one of the participants
        if (!currentUserId.equals(userId1) && !currentUserId.equals(userId2)) {
            throw new IllegalArgumentException("You can only access direct threads involving yourself");
        }
        ChatThread thread = chatService.getDirectThread(userId1, userId2);
        return ResponseEntity.ok(toThreadDto(thread));
    }

    @GetMapping("/direct/my-threads")
    public ResponseEntity<List<ChatThreadDto>> getMyDirectThreads(@CurrentUser UUID userId) {
        List<ChatThread> threads = chatService.getUserDirectThreads(userId);
        return ResponseEntity.ok(threads.stream().map(this::toThreadDto).collect(Collectors.toList()));
    }

    private ChatThreadDto toThreadDto(ChatThread thread) {
        return ChatThreadDto.builder()
                .id(thread.getId())
                .bookingId(thread.getBookingId())
                .userId1(thread.getUserId1())
                .userId2(thread.getUserId2())
                .createdAt(thread.getCreatedAt())
                .build();
    }

    private ChatMessageDto toMessageDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .threadId(message.getThreadId())
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .text(message.getText())
                .mediaUrl(message.getMediaUrl())
                .locationLat(message.getLocationLat())
                .locationLng(message.getLocationLng())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
