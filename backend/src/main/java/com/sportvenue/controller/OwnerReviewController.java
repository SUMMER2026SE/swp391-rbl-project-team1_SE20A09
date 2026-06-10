package com.sportvenue.controller;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/v1/owner/reviews")
@RequiredArgsConstructor
@Tag(name = "Owner — Review Management", description = "Owner xem và phản hồi đánh giá của khách hàng")
public class OwnerReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Lấy danh sách đánh giá của khách hàng (Owner)")
    public ResponseEntity<Page<ReviewResponse>> listOwnerReviews(
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Page<ReviewResponse> response = reviewService.getOwnerReviews(userPrincipal.getUsername(), pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Owner phản hồi đánh giá của khách hàng")
    public ResponseEntity<ReviewResponse> replyToReview(
            @PathVariable Integer id,
            @Valid @RequestBody ReplyReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ReviewResponse response = reviewService.replyToReview(id, request.getOwnerResponse(), userPrincipal.getUsername());
        return ResponseEntity.ok(response);
    }
}
