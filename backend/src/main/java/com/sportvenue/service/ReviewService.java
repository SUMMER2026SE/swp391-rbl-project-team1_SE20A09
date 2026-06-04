package com.sportvenue.service;

import com.sportvenue.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    /** Lấy toàn bộ đánh giá của khách hàng đối với các sân thuộc quản lý của một Owner. */
    List<ReviewResponse> getOwnerReviews(String ownerEmail);

    /** Lấy toàn bộ đánh giá mà một Customer đã gửi. */
    List<ReviewResponse> getCustomerReviews(String customerEmail);

    /** Owner phản hồi đánh giá của khách hàng. */
    ReviewResponse replyToReview(Integer reviewId, String ownerResponse, String ownerEmail);

    /** Customer thêm đánh giá mới */
    ReviewResponse createReview(com.sportvenue.dto.request.CreateReviewRequest request, String customerEmail);
}
