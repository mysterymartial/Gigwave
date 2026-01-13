package com.gigwave.application.chat;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.chat.ChatMessage;
import com.gigwave.infrastructure.persistence.chat.ChatMessageRepository;
import com.gigwave.domain.chat.ChatThread;
import com.gigwave.infrastructure.persistence.chat.ChatThreadRepository;
import com.gigwave.domain.chat.MessageType;
import com.gigwave.domain.gigs.Gig;
import com.gigwave.infrastructure.persistence.gigs.GigRepository;
import com.gigwave.domain.users.User;
import com.gigwave.infrastructure.persistence.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final GigRepository gigRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatThread openThreadForBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        return chatThreadRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    ChatThread thread = ChatThread.builder()
                            .bookingId(bookingId)
                            .build();
                    return chatThreadRepository.save(thread);
                });
    }

    @Transactional
    public ChatThread openDirectThread(UUID userId1, UUID userId2) {
        // Ensure users exist and are not disabled
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("User 1 not found"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("User 2 not found"));
        
        if (Boolean.TRUE.equals(user1.getIsDisabled())) {
            throw new IllegalArgumentException("Cannot create chat thread: user 1 account is disabled");
        }
        if (Boolean.TRUE.equals(user2.getIsDisabled())) {
            throw new IllegalArgumentException("Cannot create chat thread: user 2 account is disabled");
        }

        // Normalize user IDs to avoid duplicate threads (smaller ID first)
        UUID firstUserId = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID secondUserId = userId1.compareTo(userId2) < 0 ? userId2 : userId1;

        return chatThreadRepository.findByUserId1AndUserId2(firstUserId, secondUserId)
                .orElseGet(() -> {
                    ChatThread thread = ChatThread.builder()
                            .userId1(firstUserId)
                            .userId2(secondUserId)
                            .build();
                    return chatThreadRepository.save(thread);
                });
    }

    @Transactional
    public ChatMessage sendMessage(UUID threadId, UUID senderId, MessageType messageType, String text, String mediaUrl, Double locationLat, Double locationLng) {
        ChatThread thread = chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Chat thread not found"));

        // Check if sender's account is disabled
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        if (Boolean.TRUE.equals(sender.getIsDisabled())) {
            throw new IllegalArgumentException("Cannot send message: account is disabled");
        }

        // Verify sender is part of the thread
        if (thread.getBookingId() != null) {
            // Booking-based chat: verify sender is musician or organizer
            Booking booking = bookingRepository.findById(thread.getBookingId())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
            
            if (!booking.getMusicianId().equals(senderId)) {
                // Check if sender is the organizer
                Gig gig = gigRepository.findById(booking.getGigId())
                        .orElseThrow(() -> new IllegalArgumentException("Gig not found"));
                if (!gig.getOrganizerId().equals(senderId)) {
                    throw new IllegalArgumentException("Sender is not authorized to send messages in this thread");
                }
            }
        } else {
            // Direct messaging: verify sender is one of the participants
            if (thread.getUserId1() == null || thread.getUserId2() == null) {
                throw new IllegalArgumentException("Invalid direct chat thread");
            }
            if (!thread.getUserId1().equals(senderId) && !thread.getUserId2().equals(senderId)) {
                throw new IllegalArgumentException("Sender is not authorized to send messages in this thread");
            }
        }

        ChatMessage message = ChatMessage.builder()
                .threadId(threadId)
                .senderId(senderId)
                .messageType(messageType)
                .text(text)
                .mediaUrl(mediaUrl)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .build();

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> listMessages(UUID threadId) {
        return chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
    }

    public ChatThread getThreadByBookingId(UUID bookingId) {
        return chatThreadRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Chat thread not found for this booking"));
    }

    public ChatThread getDirectThread(UUID userId1, UUID userId2) {
        UUID firstUserId = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID secondUserId = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        return chatThreadRepository.findByUserId1AndUserId2(firstUserId, secondUserId)
                .orElseThrow(() -> new IllegalArgumentException("Direct chat thread not found between these users"));
    }

    public List<ChatThread> getUserDirectThreads(UUID userId) {
        List<ChatThread> threads1 = chatThreadRepository.findByUserId1(userId);
        List<ChatThread> threads2 = chatThreadRepository.findByUserId2(userId);
        threads1.addAll(threads2);
        return threads1.stream().distinct().collect(Collectors.toList());
    }
}
