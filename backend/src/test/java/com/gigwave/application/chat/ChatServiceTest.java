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
import com.gigwave.domain.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {
    @Mock
    private ChatThreadRepository chatThreadRepository;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @Mock
    private GigRepository gigRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ChatService chatService;
    
    private UUID bookingId;
    private UUID threadId;
    private UUID senderId;
    private UUID userId1;
    private UUID userId2;
    private UUID musicianId;
    private UUID organizerId;
    private UUID gigId;
    
    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        threadId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        musicianId = UUID.randomUUID();
        organizerId = UUID.randomUUID();
        gigId = UUID.randomUUID();
    }
    
    @Test
    void testOpenThreadForBooking_NewThread() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(new Booking()));
        when(chatThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(invocation -> {
            ChatThread thread = invocation.getArgument(0);
            thread.setId(threadId);
            return thread;
        });
        
        ChatThread result = chatService.openThreadForBooking(bookingId);
        
        assertNotNull(result);
        verify(chatThreadRepository).save(any(ChatThread.class));
    }
    
    @Test
    void testOpenThreadForBooking_ExistingThread() {
        ChatThread existingThread = ChatThread.builder()
                .id(threadId)
                .bookingId(bookingId)
                .build();
        
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(new Booking()));
        when(chatThreadRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingThread));
        
        ChatThread result = chatService.openThreadForBooking(bookingId);
        
        assertEquals(existingThread, result);
        verify(chatThreadRepository, never()).save(any(ChatThread.class));
    }
    
    @Test
    void testSendMessage_Success_DirectChat() {
        // Direct messaging: sender is userId1
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        User sender = User.builder().id(userId1).isDisabled(false).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // Set senderId to userId1 (one of the participants)
        ChatMessage result = chatService.sendMessage(threadId, userId1, MessageType.TEXT, "Hello", null, null, null);
        
        assertNotNull(result);
        assertEquals("Hello", result.getText());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void testSendMessage_Success_BookingChat() {
        // Booking-based chat: sender is musician
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .bookingId(bookingId)
                .build();
        
        Booking booking = Booking.builder()
                .id(bookingId)
                .gigId(gigId)
                .musicianId(musicianId)
                .build();
        
        Gig gig = Gig.builder()
                .id(gigId)
                .organizerId(organizerId)
                .build();
        
        User sender = User.builder().id(musicianId).isDisabled(false).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        // gigRepository.findById is not needed here since sender is musician (not organizer)
        when(userRepository.findById(musicianId)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        ChatMessage result = chatService.sendMessage(threadId, musicianId, MessageType.TEXT, "Hello", null, null, null);
        
        assertNotNull(result);
        assertEquals("Hello", result.getText());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    void testSendMessage_WithMedia_DirectChat() {
        // Boundary: media message in direct chat
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        User sender = User.builder().id(userId1).isDisabled(false).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        ChatMessage result = chatService.sendMessage(threadId, userId1, MessageType.IMAGE, null, "https://example.com/image.jpg", null, null);
        
        assertNotNull(result);
        assertEquals(MessageType.IMAGE, result.getMessageType());
        assertEquals("https://example.com/image.jpg", result.getMediaUrl());
    }
    
    @Test
    void testSendMessage_WithLocation_DirectChat() {
        // Boundary: location message in direct chat
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        User sender = User.builder().id(userId1).isDisabled(false).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        ChatMessage result = chatService.sendMessage(threadId, userId1, MessageType.LOCATION, null, null, 6.5244, 3.3792);
        
        assertNotNull(result);
        assertEquals(MessageType.LOCATION, result.getMessageType());
        assertEquals(6.5244, result.getLocationLat());
        assertEquals(3.3792, result.getLocationLng());
    }
    
    @Test
    void testSendMessage_ThreadNotFound() {
        // Boundary: non-existent thread
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(threadId, senderId, MessageType.TEXT, "Hello", null, null, null));
    }

    @Test
    void testSendMessage_DisabledAccount() {
        // Edge case: disabled account cannot send messages
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        User disabledUser = User.builder().id(userId1).isDisabled(true).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(userRepository.findById(userId1)).thenReturn(Optional.of(disabledUser));
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(threadId, userId1, MessageType.TEXT, "Hello", null, null, null),
                "Cannot send message: account is disabled");
    }

    @Test
    void testSendMessage_UnauthorizedSender_DirectChat() {
        // Edge case: sender is not part of the direct chat thread
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        UUID unauthorizedUserId = UUID.randomUUID();
        User unauthorizedUser = User.builder().id(unauthorizedUserId).isDisabled(false).build();
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(unauthorizedUser));
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(threadId, unauthorizedUserId, MessageType.TEXT, "Hello", null, null, null),
                "Sender is not authorized to send messages in this thread");
    }

    @Test
    void testSendMessage_UnauthorizedSender_BookingChat() {
        // Edge case: sender is not musician or organizer in booking chat
        ChatThread thread = ChatThread.builder()
                .id(threadId)
                .bookingId(bookingId)
                .build();
        
        Booking booking = Booking.builder()
                .id(bookingId)
                .gigId(gigId)
                .musicianId(musicianId)
                .build();
        
        Gig gig = Gig.builder()
                .id(gigId)
                .organizerId(organizerId)
                .build();
        
        UUID unauthorizedUserId = UUID.randomUUID();
        User unauthorizedUser = User.builder().id(unauthorizedUserId).isDisabled(false).build();
        
        when(chatThreadRepository.findById(threadId)).thenReturn(Optional.of(thread));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(gigRepository.findById(gigId)).thenReturn(Optional.of(gig));
        when(userRepository.findById(unauthorizedUserId)).thenReturn(Optional.of(unauthorizedUser));
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.sendMessage(threadId, unauthorizedUserId, MessageType.TEXT, "Hello", null, null, null),
                "Sender is not authorized to send messages in this thread");
    }

    @Test
    void testOpenDirectThread_Success() {
        User user1 = User.builder().id(userId1).isDisabled(false).build();
        User user2 = User.builder().id(userId2).isDisabled(false).build();
        
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(chatThreadRepository.findByUserId1AndUserId2(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.empty());
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(invocation -> {
            ChatThread thread = invocation.getArgument(0);
            thread.setId(threadId);
            return thread;
        });
        
        ChatThread result = chatService.openDirectThread(userId1, userId2);
        
        assertNotNull(result);
        assertNotNull(result.getUserId1());
        assertNotNull(result.getUserId2());
        verify(chatThreadRepository).save(any(ChatThread.class));
    }

    @Test
    void testOpenDirectThread_ExistingThread() {
        User user1 = User.builder().id(userId1).isDisabled(false).build();
        User user2 = User.builder().id(userId2).isDisabled(false).build();
        
        ChatThread existingThread = ChatThread.builder()
                .id(threadId)
                .userId1(userId1)
                .userId2(userId2)
                .build();
        
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(chatThreadRepository.findByUserId1AndUserId2(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(existingThread));
        
        ChatThread result = chatService.openDirectThread(userId1, userId2);
        
        assertEquals(existingThread, result);
        verify(chatThreadRepository, never()).save(any(ChatThread.class));
    }

    @Test
    void testOpenDirectThread_DisabledUser() {
        // Edge case: disabled user cannot create chat thread
        User user1 = User.builder().id(userId1).isDisabled(true).build();
        User user2 = User.builder().id(userId2).isDisabled(false).build();
        
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.openDirectThread(userId1, userId2),
                "Cannot create chat thread: user 1 account is disabled");
    }

    @Test
    void testOpenDirectThread_NormalizesUserIds() {
        // Edge case: User IDs should be normalized (smaller ID first)
        User user1 = User.builder().id(userId1).isDisabled(false).build();
        User user2 = User.builder().id(userId2).isDisabled(false).build();
        
        // userId2 < userId1 (lexicographically)
        UUID smallerId = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID largerId = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));
        when(chatThreadRepository.findByUserId1AndUserId2(smallerId, largerId))
                .thenReturn(Optional.empty());
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(invocation -> {
            ChatThread thread = invocation.getArgument(0);
            thread.setId(threadId);
            return thread;
        });
        
        ChatThread result = chatService.openDirectThread(userId1, userId2);
        
        assertNotNull(result);
        verify(chatThreadRepository).findByUserId1AndUserId2(smallerId, largerId);
    }

    @Test
    void testOpenDirectThread_UserNotFound() {
        // Boundary: user not found
        when(userRepository.findById(userId1)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.openDirectThread(userId1, userId2),
                "User 1 not found");
    }

    @Test
    void testGetDirectThread_Success() {
        UUID firstUserId = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID secondUserId = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        ChatThread existingThread = ChatThread.builder()
                .id(threadId)
                .userId1(firstUserId)
                .userId2(secondUserId)
                .build();
        
        when(chatThreadRepository.findByUserId1AndUserId2(firstUserId, secondUserId))
                .thenReturn(Optional.of(existingThread));
        
        ChatThread result = chatService.getDirectThread(userId1, userId2);
        
        assertEquals(existingThread, result);
    }

    @Test
    void testGetDirectThread_NotFound() {
        UUID firstUserId = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        UUID secondUserId = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        
        when(chatThreadRepository.findByUserId1AndUserId2(firstUserId, secondUserId))
                .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class,
                () -> chatService.getDirectThread(userId1, userId2),
                "Direct chat thread not found between these users");
    }

    @Test
    void testGetUserDirectThreads_Success() {
        ChatThread thread1 = ChatThread.builder().id(UUID.randomUUID()).userId1(userId1).userId2(userId2).build();
        ChatThread thread2 = ChatThread.builder().id(UUID.randomUUID()).userId1(userId2).userId2(userId1).build();
        
        // Use mutable ArrayList instead of Arrays.asList() to avoid UnsupportedOperationException
        when(chatThreadRepository.findByUserId1(userId1)).thenReturn(new ArrayList<>(Arrays.asList(thread1)));
        when(chatThreadRepository.findByUserId2(userId1)).thenReturn(new ArrayList<>(Arrays.asList(thread2)));
        
        List<ChatThread> result = chatService.getUserDirectThreads(userId1);
        
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    void testGetUserDirectThreads_Empty() {
        // Use mutable ArrayList instead of Arrays.asList() to avoid UnsupportedOperationException
        when(chatThreadRepository.findByUserId1(userId1)).thenReturn(new ArrayList<>());
        when(chatThreadRepository.findByUserId2(userId1)).thenReturn(new ArrayList<>());
        
        List<ChatThread> result = chatService.getUserDirectThreads(userId1);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testListMessages_Empty() {
        // Boundary: no messages
        when(chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)).thenReturn(Arrays.asList());
        
        List<ChatMessage> result = chatService.listMessages(threadId);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testListMessages_MultipleMessages() {
        ChatMessage msg1 = ChatMessage.builder().id(UUID.randomUUID()).text("First").build();
        ChatMessage msg2 = ChatMessage.builder().id(UUID.randomUUID()).text("Second").build();
        
        when(chatMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId))
                .thenReturn(Arrays.asList(msg1, msg2));
        
        List<ChatMessage> result = chatService.listMessages(threadId);
        
        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getText());
    }
}
