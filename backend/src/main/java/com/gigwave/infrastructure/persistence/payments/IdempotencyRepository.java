package com.gigwave.infrastructure.persistence.payments;

import com.gigwave.domain.payments.IdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRepository extends MongoRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByKey(String key);
}
