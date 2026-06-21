package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewResponse createReview(Integer bookingId, CreateReviewRequest request, String userEmail) {
        log.info("Creating review for booking {} by user {}", bookingId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getUserId().equals(user.getUserId())) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sân do chính mình đặt.");
        }

        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá khi sân đã sử dụng xong (Trạng thái: Hoàn thành).");
        }

        if (reviewRepository.existsByBookingBookingId(booking.getBookingId())) {
            throw new BadRequestException("Bạn đã đánh giá sân này rồi.");
        }

        Review review = Review.builder()
                .booking(booking)
                .user(user)
                .stadium(booking.getStadium())
                .ratingScore(request.getRatingScore())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        // Calculate and update stadium average rating
        Optional<Double> avgRatingOpt = reviewRepository.calculateAverageRating(booking.getStadium().getStadiumId());
        if (avgRatingOpt.isPresent()) {
            Stadium stadium = booking.getStadium();
            stadium.setAverageRating(java.math.BigDecimal.valueOf(avgRatingOpt.get()));
            // stadiumRepository is needed to save the updated stadium, or we rely on transactional save if stadium is attached.
            // Since it's transactional, modifying stadium should flush automatically if it's managed.
        }

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewResponse> getStadiumReviews(Integer stadiumId, org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findByStadiumStadiumIdOrderByCreatedAtDesc(stadiumId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewResponse> getMyReviews(String email, org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findByUserEmailOrderByCreatedAtDesc(email, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewResponse> getOwnerReviews(String ownerEmail, org.springframework.data.domain.Pageable pageable) {
        return reviewRepository.findByStadiumOwnerUserEmailOrderByCreatedAtDesc(ownerEmail, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(Integer reviewId, String replyMessage, String ownerEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getStadium().getOwner().getUser().getEmail().equals(ownerEmail)) {
            throw new BadRequestException("Bạn không phải chủ sân này, không có quyền phản hồi.");
        }

        review.setOwnerResponse(replyMessage);
        review = reviewRepository.save(review);
        return mapToResponse(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .bookingId(review.getBooking().getBookingId())
                .stadiumId(review.getStadium().getStadiumId())
                .reviewerName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .ratingScore(review.getRatingScore())
                .comment(review.getComment())
                .ownerResponse(review.getOwnerResponse())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Integer reviewId, CreateReviewRequest request, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID " + reviewId));

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRatingScore(request.getRatingScore());
        review.setComment(request.getComment().trim());
        review = reviewRepository.save(review);

        // Recalculate and update stadium average rating
        Optional<Double> avgRatingOpt = reviewRepository.calculateAverageRating(review.getStadium().getStadiumId());
        if (avgRatingOpt.isPresent()) {
            Stadium stadium = review.getStadium();
            stadium.setAverageRating(java.math.BigDecimal.valueOf(avgRatingOpt.get()));
        }

        return mapToResponse(review);
    }
}
