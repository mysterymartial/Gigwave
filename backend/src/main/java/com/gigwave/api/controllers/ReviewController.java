package com.gigwave.api.controllers;

import com.gigwave.api.dto.reviews.ReviewDto;
import com.gigwave.application.reviews.ReviewService;
import com.gigwave.domain.reviews.Review;
import com.gigwave.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody ReviewDto dto,
            @CurrentUser UUID reviewerId
    ) {
        Review review = reviewService.createReview(
                dto.getBookingId(),
                reviewerId,
                dto.getReviewedUserId(),
                dto.getRating(),
                dto.getComment()
        );
        return ResponseEntity.ok(toDto(review));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDto>> listReviewsForUser(@PathVariable UUID userId) {
        List<Review> reviews = reviewService.listReviewsForUser(userId);
        return ResponseEntity.ok(reviews.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<List<ReviewDto>> listReviewsForBooking(@PathVariable UUID bookingId) {
        List<Review> reviews = reviewService.listReviewsForBooking(bookingId);
        return ResponseEntity.ok(reviews.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/user/{userId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID userId) {
        Double average = reviewService.computeAverageRating(userId);
        return ResponseEntity.ok(average);
    }

    private ReviewDto toDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .bookingId(review.getBookingId())
                .reviewerId(review.getReviewerId())
                .reviewedUserId(review.getReviewedUserId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}





