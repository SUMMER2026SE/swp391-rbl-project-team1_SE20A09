package com.sportvenue.service;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.ReviewResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    ReviewResponse createReview(Integer bookingId, CreateReviewRequest request, String userEmail);
    Page<ReviewResponse> getStadiumReviews(Integer stadiumId, Pageable pageable);
    Page<ReviewResponse> getMyReviews(String email, Pageable pageable);
    Page<ReviewResponse> getOwnerReviews(String ownerEmail, Pageable pageable);
    ReviewResponse replyToReview(Integer reviewId, String replyMessage, String ownerEmail);
}
