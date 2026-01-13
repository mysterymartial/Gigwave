package com.gigwave.infrastructure.persistence.payments;

import com.gigwave.domain.payments.BankAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends MongoRepository<BankAccount, UUID> {
    List<BankAccount> findByUserId(UUID userId);
    Optional<BankAccount> findByUserIdAndIsPayoutDefaultTrue(UUID userId);
}
