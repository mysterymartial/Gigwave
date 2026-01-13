package com.gigwave.infrastructure.persistence.gigs;

import com.gigwave.domain.gigs.Gig;
import com.gigwave.domain.gigs.GigStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GigRepository extends MongoRepository<Gig, UUID> {
    List<Gig> findByOrganizerId(UUID organizerId);
    List<Gig> findByStatus(GigStatus status);
    List<Gig> findByStatusAndEventDateAfter(GigStatus status, LocalDateTime eventDate);
    
    
}
