package com.gigwave.application.reviews;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.domain.bookings.Booking;
import com.gigwave.domain.reviews.Review;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.infrastructure.persistence.reviews.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public Review createReview(UUID bookingId, UUID reviewerId, UUID reviewedUserId, Integer rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (!booking.getMusicianId().equals(reviewerId) && !booking.getGigId().equals(reviewerId)) {
            throw new IllegalArgumentException("Reviewer must be part of this booking");
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .reviewerId(reviewerId)
                .reviewedUserId(reviewedUserId)
                .rating(rating)
                .comment(comment)
                .build();

        review = reviewRepository.save(review);

        // Update musician rating if reviewed user is a musician
        updateMusicianRating(reviewedUserId);

        return review;
    }

    public List<Review> listReviewsForUser(UUID userId) {
        return reviewRepository.findByReviewedUserId(userId);
    }

    public List<Review> listReviewsForBooking(UUID bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    public Double computeAverageRating(UUID userId) {
        Double average = reviewRepository.getAverageRatingByUserId(userId);
        return average != null ? average : 0.0;
    }

    private void updateMusicianRating(UUID userId) {
        Double averageRating = computeAverageRating(userId);
        // This would typically update the MusicianProfile rating
        // For now, we'll leave it as the rating is computed on-demand
    }
}




