package com.gigwave.infrastructure.persistence.disputes;

import com.gigwave.domain.disputes.Dispute;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeRepository extends MongoRepository<Dispute, UUID> {
    List<Dispute> findByBookingId(UUID bookingId);
    List<Dispute> findByRaisedBy(UUID raisedBy);
}
