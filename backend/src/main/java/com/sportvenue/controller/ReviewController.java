package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Đánh giá sân bóng")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/bookings/{bookingId}/reviews")
    @Operation(summary = "Tạo đánh giá", description = "Đánh giá chỉ được tạo sau khi đặt sân đã hoàn thành.")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Integer bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.createReview(bookingId, request, userPrincipal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reviews/me")
    @Operation(summary = "Lấy đánh giá của tôi", description = "Lấy danh sách đánh giá do user đang đăng nhập viết.")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reviewService.getMyReviews(userPrincipal.getUsername(), pageable));
    }

    @GetMapping("/public/stadiums/{stadiumId}/reviews")
    @Operation(summary = "Lấy đánh giá của sân", description = "Lấy danh sách đánh giá của một sân cụ thể.")
    public ResponseEntity<Page<ReviewResponse>> getStadiumReviews(
            @PathVariable Integer stadiumId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getStadiumReviews(stadiumId, pageable));
    }

    @GetMapping("/owner/reviews")
    @Operation(summary = "Lấy đánh giá của các sân (Dành cho Owner)", description = "Lấy danh sách đánh giá của các sân thuộc owner đang đăng nhập.")
    public ResponseEntity<Page<ReviewResponse>> getOwnerReviews(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(reviewService.getOwnerReviews(userPrincipal.getUsername(), pageable));
    }

    @PostMapping("/owner/reviews/{reviewId}/reply")
    @Operation(summary = "Phản hồi đánh giá (Dành cho Owner)", description = "Chủ sân phản hồi đánh giá của khách hàng.")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Integer reviewId,
            @RequestBody java.util.Map<String, String> requestBody,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String replyMessage = requestBody.get("replyMessage");
        return ResponseEntity.ok(reviewService.replyToReview(reviewId, replyMessage, userPrincipal.getUsername()));
    }
}
