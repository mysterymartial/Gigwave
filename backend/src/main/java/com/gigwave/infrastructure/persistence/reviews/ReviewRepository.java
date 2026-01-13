package com.gigwave.infrastructure.persistence.reviews;

import com.gigwave.domain.reviews.Review;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends MongoRepository<Review, UUID> {
    List<Review> findByReviewedUserId(UUID reviewedUserId);
    List<Review> findByBookingId(UUID bookingId);
    
    @Aggregation(pipeline = {
        "{ $match: { reviewedUserId: ?0 } }",
        "{ $group: { _id: null, avgRating: { $avg: '$rating' } } }"
    })
    Double getAverageRatingByUserId(UUID userId);
}
