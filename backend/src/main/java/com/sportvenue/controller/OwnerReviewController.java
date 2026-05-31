package com.sportvenue.controller;

import com.sportvenue.dto.request.ReviewReplyRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.OwnerReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý đánh giá dành cho Owner.
 * UC-OWN-08: Xem và phản hồi đánh giá của khách hàng.
 */
@RestController
@RequestMapping("/api/v1/owner/reviews")
@RequiredArgsConstructor
@Tag(name = "Owner — Review Management",
     description = "Xem và phản hồi đánh giá của khách hàng")
@Slf4j
public class OwnerReviewController {

    private final OwnerReviewService ownerReviewService;

    /**
     * Xem danh sách đánh giá của tất cả sân.
     * Hỗ trợ filter theo sân cụ thể và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Xem danh sách đánh giá",
               description = "Owner xem tất cả đánh giá của sân mình. "
                       + "Hỗ trợ filter theo stadiumId.")
    public ResponseEntity<Page<ReviewResponse>> getReviews(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) Integer stadiumId,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ReviewResponse> result = ownerReviewService.getOwnerReviews(
                userPrincipal.getUser().getUserId(),
                stadiumId, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * UC-OWN-08: Phản hồi đánh giá của khách hàng.
     * Mỗi đánh giá chỉ được phản hồi 1 lần.
     */
    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Phản hồi đánh giá",
               description = "Owner phản hồi đánh giá. Chỉ phản hồi được 1 lần.")
    public ResponseEntity<ReviewResponse> replyToReview(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Integer reviewId,
            @Valid @RequestBody ReviewReplyRequest request) {

        ReviewResponse result = ownerReviewService.replyToReview(
                userPrincipal.getUser().getUserId(),
                reviewId, request);
        return ResponseEntity.ok(result);
    }
}
