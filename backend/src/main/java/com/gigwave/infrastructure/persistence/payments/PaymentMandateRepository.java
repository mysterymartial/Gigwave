package com.gigwave.infrastructure.persistence.payments;

import com.gigwave.domain.payments.MandateStatus;
import com.gigwave.domain.payments.PaymentMandate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMandateRepository extends MongoRepository<PaymentMandate, UUID> {
    List<PaymentMandate> findByUserId(UUID userId);
    Optional<PaymentMandate> findByUserIdAndStatus(UUID userId, MandateStatus status);
    Optional<PaymentMandate> findByMandateRef(String mandateRef);
}
