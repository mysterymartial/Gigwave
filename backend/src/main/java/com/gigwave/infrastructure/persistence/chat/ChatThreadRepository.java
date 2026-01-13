package com.gigwave.infrastructure.persistence.chat;

import com.gigwave.domain.chat.ChatThread;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatThreadRepository extends MongoRepository<ChatThread, UUID> {
    Optional<ChatThread> findByBookingId(UUID bookingId);
    Optional<ChatThread> findByUserId1AndUserId2(UUID userId1, UUID userId2);
    List<ChatThread> findByUserId1(UUID userId1);
    List<ChatThread> findByUserId2(UUID userId2);
}
