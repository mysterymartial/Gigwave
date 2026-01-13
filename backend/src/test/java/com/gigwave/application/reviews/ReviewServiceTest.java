package com.gigwave.application.reviews;

import com.gigwave.domain.bookings.Booking;
import com.gigwave.infrastructure.persistence.bookings.BookingRepository;
import com.gigwave.domain.reviews.Review;
import com.gigwave.infrastructure.persistence.reviews.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private BookingRepository bookingRepository;
    
    @InjectMocks
    private ReviewService reviewService;
    
    private UUID bookingId;
    private UUID reviewerId;
    private UUID reviewedUserId;
    private Booking testBooking;
    
    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        reviewedUserId = UUID.randomUUID();
        
        testBooking = Booking.builder()
                .id(bookingId)
                .musicianId(reviewedUserId)
                .build();
    }
    
    @Test
    void testCreateReview_Success() {
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(4.5);
        
        Review result = reviewService.createReview(bookingId, reviewerId, reviewedUserId, 5, "Great!");
        
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Great!", result.getComment());
        verify(reviewRepository).save(any(Review.class));
    }
    
    @Test
    void testCreateReview_MinimumRating() {
        // Boundary: minimum rating (1)
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(1.0);
        
        Review result = reviewService.createReview(bookingId, reviewerId, reviewedUserId, 1, "Poor");
        
        assertEquals(1, result.getRating());
    }
    
    @Test
    void testCreateReview_MaximumRating() {
        // Boundary: maximum rating (5)
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(5.0);
        
        Review result = reviewService.createReview(bookingId, reviewerId, reviewedUserId, 5, "Excellent");
        
        assertEquals(5, result.getRating());
    }
    
    @Test
    void testCreateReview_InvalidReviewer() {
        // Boundary: reviewer not part of booking
        UUID invalidReviewerId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.createReview(bookingId, invalidReviewerId, reviewedUserId, 5, "Comment"));
    }
    
    @Test
    void testCreateReview_EmptyComment() {
        // Boundary: empty comment
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(4.0);
        
        Review result = reviewService.createReview(bookingId, reviewerId, reviewedUserId, 4, "");
        
        assertNotNull(result);
        assertEquals("", result.getComment());
    }
    
    @Test
    void testComputeAverageRating_NoReviews() {
        // Boundary: no reviews
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(null);
        
        Double result = reviewService.computeAverageRating(reviewedUserId);
        
        assertEquals(0.0, result);
    }
    
    @Test
    void testComputeAverageRating_WithReviews() {
        when(reviewRepository.getAverageRatingByUserId(reviewedUserId)).thenReturn(4.25);
        
        Double result = reviewService.computeAverageRating(reviewedUserId);
        
        assertEquals(4.25, result);
    }
}




