package com.sportvenue.controller;

import com.sportvenue.dto.request.VenueReviewRequest;
import com.sportvenue.dto.response.VenueReviewResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.VenueReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Customer — Venue Reviews", description = "Quản lý đánh giá sân của khách hàng (UC-CUS-07)")
public class CustomerVenueReviewController {

    private final VenueReviewService venueReviewService;

    @PostMapping("/{venueId}/reviews")
    @PreAuthorize("hasRole('Customer')")
    @Operation(summary = "Customer viết đánh giá cho sân sau khi chơi (UC-CUS-07)")
    public ResponseEntity<VenueReviewResponse> createVenueReview(
            @PathVariable Integer venueId,
            @Valid @RequestBody VenueReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        VenueReviewResponse response = venueReviewService.createReview(venueId, request, userPrincipal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
