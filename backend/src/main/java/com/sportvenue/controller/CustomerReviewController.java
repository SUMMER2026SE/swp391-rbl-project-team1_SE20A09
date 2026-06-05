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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer — Review Management", description = "Khách hàng quản lý đánh giá của mình")
public class CustomerReviewController {

    private final ReviewService reviewService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy danh sách đánh giá của mình")
    public ResponseEntity<List<ReviewResponse>> listCustomerReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ReviewResponse> response = reviewService.getCustomerReviews(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Customer tạo đánh giá mới cho đơn đặt sân")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.createReview(request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
