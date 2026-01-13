package com.gigwave.infrastructure.persistence.payments;

import com.gigwave.domain.payments.Payout;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends MongoRepository<Payout, UUID> {
    List<Payout> findByMusicianId(UUID musicianId);
    List<Payout> findByBookingId(UUID bookingId);
    Optional<Payout> findByProviderRef(String providerRef);
}
