package com.gigwave.infrastructure.persistence.users;

import com.gigwave.domain.users.KycDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycDocumentRepository extends MongoRepository<KycDocument, UUID> {
    List<KycDocument> findByUserId(UUID userId);
    List<KycDocument> findByUserIdAndDocumentType(UUID userId, String documentType);
}
