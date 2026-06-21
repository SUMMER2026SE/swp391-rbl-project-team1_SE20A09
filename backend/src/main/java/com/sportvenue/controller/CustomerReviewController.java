package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer — Review Management", description = "Khách hàng quản lý đánh giá của mình")
public class CustomerReviewController {

    private final ReviewService reviewService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy danh sách đánh giá của mình")
    public ResponseEntity<Page<ReviewResponse>> listCustomerReviews(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Page<ReviewResponse> response = reviewService.getMyReviews(userPrincipal.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bookings/{bookingId}/reviews")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Customer tạo đánh giá mới cho đơn đặt sân")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Integer bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.createReview(bookingId, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "UC-CUS-08: Sửa đánh giá")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Integer reviewId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.updateReview(reviewId, request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
