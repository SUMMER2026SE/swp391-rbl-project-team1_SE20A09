package com.sportvenue.service;

import com.sportvenue.dto.request.ReviewReplyRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ đánh giá cho Owner.
 * UC-OWN-08: Xem và phản hồi đánh giá của khách hàng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerReviewService {

    private final ReviewRepository reviewRepository;
    private final OwnerRepository ownerRepository;
    private final StadiumRepository stadiumRepository;

    /**
     * Lấy danh sách đánh giá của tất cả sân thuộc Owner.
     *
     * @param userId ID của Owner
     * @param stadiumId (optional) filter theo sân cụ thể
     * @param pageable phân trang
     * @return trang review responses
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getOwnerReviews(
            Integer userId, Integer stadiumId, Pageable pageable) {

        Owner owner = findOwnerByUserId(userId);

        if (stadiumId != null) {
            validateStadiumOwnership(stadiumId, owner.getOwnerId());
            return reviewRepository
                    .findByStadiumStadiumIdOrderByCreatedAtDesc(
                            stadiumId, pageable)
                    .map(this::toReviewResponse);
        }

        // Lấy tất cả reviews từ tất cả sân của owner
        List<Stadium> stadiums = stadiumRepository
                .findByOwnerOwnerIdAndStadiumStatusNot(
                        owner.getOwnerId(),
                        com.sportvenue.entity.enums.StadiumStatus.CLOSED);
        List<Integer> stadiumIds = stadiums.stream()
                .map(Stadium::getStadiumId)
                .toList();

        if (stadiumIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return reviewRepository
                .findByStadiumStadiumIdInOrderByCreatedAtDesc(
                        stadiumIds, pageable)
                .map(this::toReviewResponse);
    }

    /**
     * UC-OWN-08: Owner phản hồi đánh giá của khách hàng.
     *
     * @param userId ID của Owner
     * @param reviewId ID của review cần phản hồi
     * @param request nội dung phản hồi
     * @return review response sau khi cập nhật
     */
    @Transactional
    public ReviewResponse replyToReview(
            Integer userId, Integer reviewId,
            ReviewReplyRequest request) {

        Owner owner = findOwnerByUserId(userId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đánh giá: " + reviewId));

        // Kiểm tra quyền sở hữu sân
        validateStadiumOwnership(
                review.getStadium().getStadiumId(),
                owner.getOwnerId());

        // Kiểm tra đã phản hồi chưa
        if (review.getOwnerResponse() != null
                && !review.getOwnerResponse().isBlank()) {
            throw new BadRequestException(
                    "Bạn đã phản hồi đánh giá này rồi. "
                    + "Không thể phản hồi lần thứ hai.");
        }

        review.setOwnerResponse(request.getOwnerResponse());
        Review saved = reviewRepository.save(review);
        log.info("💬 Owner đã phản hồi review #{}", saved.getReviewId());
        return toReviewResponse(saved);
    }

    // ── Helper methods ────────────────────────────────────────────

    private Owner findOwnerByUserId(Integer userId) {
        return ownerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ chủ sân cho user: " + userId));
    }

    private void validateStadiumOwnership(
            Integer stadiumId, Integer ownerId) {
        Stadium stadium = stadiumRepository.findById(stadiumId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sân: " + stadiumId));
        if (!stadium.getOwner().getOwnerId().equals(ownerId)) {
            throw new BadRequestException(
                    "Bạn không có quyền quản lý sân này.");
        }
    }

    private ReviewResponse toReviewResponse(Review review) {
        User reviewer = review.getUser();
        Stadium stadium = review.getStadium();

        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .bookingId(review.getBooking().getBookingId())
                .reviewer(ReviewResponse.ReviewerInfo.builder()
                        .userId(reviewer.getUserId())
                        .fullName(reviewer.getLastName()
                                + " " + reviewer.getFirstName())
                        .avatarUrl(reviewer.getAvatarUrl())
                        .build())
                .stadium(ReviewResponse.StadiumSummary.builder()
                        .stadiumId(stadium.getStadiumId())
                        .stadiumName(stadium.getStadiumName())
                        .build())
                .ratingScore(review.getRatingScore())
                .comment(review.getComment())
                .ownerResponse(review.getOwnerResponse())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
