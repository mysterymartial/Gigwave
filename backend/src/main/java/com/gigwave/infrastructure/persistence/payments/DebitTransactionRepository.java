package com.gigwave.infrastructure.persistence.payments;

import com.gigwave.domain.payments.DebitStatus;
import com.gigwave.domain.payments.DebitTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DebitTransactionRepository extends MongoRepository<DebitTransaction, UUID> {
    List<DebitTransaction> findByBookingId(UUID bookingId);
    Optional<DebitTransaction> findByProviderRef(String providerRef);
    List<DebitTransaction> findByStatus(DebitStatus status);
}
