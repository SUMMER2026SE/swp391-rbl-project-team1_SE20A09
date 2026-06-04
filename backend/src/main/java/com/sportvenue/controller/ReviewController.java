package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.request.ReplyReviewRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Quản lý và phản hồi đánh giá của khách hàng")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/owner/reviews")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Lấy danh sách đánh giá của khách hàng (Owner)")
    public ResponseEntity<List<ReviewResponse>> listOwnerReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ReviewResponse> response = reviewService.getOwnerReviews(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/my")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Khách hàng lấy danh sách đánh giá của mình")
    public ResponseEntity<List<ReviewResponse>> listCustomerReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ReviewResponse> response = reviewService.getCustomerReviews(userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/owner/reviews/{id}/reply")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner phản hồi đánh giá của khách hàng")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.replyToReview(id, request.getOwnerResponse(), userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Customer tạo đánh giá mới cho đơn đặt sân")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.createReview(request, userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
