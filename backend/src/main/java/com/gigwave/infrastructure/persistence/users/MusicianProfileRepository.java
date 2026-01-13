package com.gigwave.infrastructure.persistence.users;

import com.gigwave.domain.users.MusicianProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MusicianProfileRepository extends MongoRepository<MusicianProfile, UUID> {
    Optional<MusicianProfile> findByUserId(UUID userId);
}
