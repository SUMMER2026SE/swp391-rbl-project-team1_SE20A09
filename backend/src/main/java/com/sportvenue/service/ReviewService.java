package com.sportvenue.service;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.EligibleBookingResponse;
import com.sportvenue.dto.response.ReviewResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(Integer bookingId, CreateReviewRequest request, String userEmail);

    Page<ReviewResponse> getStadiumReviews(Integer stadiumId, Pageable pageable);

    Page<ReviewResponse> getMyReviews(String email, Pageable pageable);

    Page<ReviewResponse> getOwnerReviews(String ownerEmail, Pageable pageable);

    ReviewResponse replyToReview(Integer reviewId, String replyMessage, String ownerEmail);

    /**
     * UC-CUS-07: Lấy danh sách booking COMPLETED mà chưa được review cho một sân cụ thể.
     * Dùng để FE xác định user có đủ điều kiện viết review hay không.
     */
    List<EligibleBookingResponse> getEligibleBookingsForReview(
            Integer stadiumId, String userEmail);
}
