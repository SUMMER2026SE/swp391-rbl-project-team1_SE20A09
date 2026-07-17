package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.EligibleBookingResponse;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ForbiddenException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ReviewService;
import com.sportvenue.service.CustomerNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CustomerNotificationService customerNotificationService;
    private final StadiumComplexRepository stadiumComplexRepository;

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

        // Calculate and update stadium + complex average rating and review counts
        updateStadiumAndComplexRating(booking.getStadium());

        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getStadiumReviews(Integer stadiumId, Pageable pageable) {
        return reviewRepository.findByStadiumStadiumIdOrderByCreatedAtDesc(stadiumId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(String email, Pageable pageable) {
        return reviewRepository.findByUserEmailOrderByCreatedAtDesc(email, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getOwnerReviews(String ownerEmail, Pageable pageable) {
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

        try {
            customerNotificationService.notifyReviewOwnerResponded(review.getUser().getUserId(), review, replyMessage);
        } catch (Exception ex) {
            log.warn("Failed to emit review owner responded notification for review {}", reviewId, ex);
        }

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
        log.info("Updating review {} by user {}", reviewId, userEmail);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá với ID " + reviewId));

        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new ForbiddenException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRatingScore(request.getRatingScore());
        review.setComment(request.getComment().trim());
        review = reviewRepository.save(review);

        // Calculate and update stadium + complex average rating and review counts
        updateStadiumAndComplexRating(review.getStadium());

        return mapToResponse(review);
    }

    private void updateStadiumAndComplexRating(Stadium stadium) {
        Optional<Double> avgRatingOpt = reviewRepository.calculateAverageRating(stadium.getStadiumId());
        long reviewCount = reviewRepository.countByStadiumStadiumId(stadium.getStadiumId());
        if (avgRatingOpt.isPresent()) {
            stadium.setAverageRating(BigDecimal.valueOf(avgRatingOpt.get()));
            stadium.setReviewCount((int) reviewCount);
        } else {
            stadium.setAverageRating(BigDecimal.valueOf(5.0));
            stadium.setReviewCount(0);
        }

        if (stadium.getComplex() != null) {
            StadiumComplex complex = stadium.getComplex();
            Optional<Double> complexAvgOpt = reviewRepository.calculateAverageRatingForComplex(complex.getComplexId());
            long complexReviewCount = reviewRepository.countByStadiumComplexComplexId(complex.getComplexId());
            if (complexAvgOpt.isPresent()) {
                complex.setAverageRating(BigDecimal.valueOf(complexAvgOpt.get()));
                complex.setReviewCount((int) complexReviewCount);
            } else {
                complex.setAverageRating(BigDecimal.valueOf(5.0));
                complex.setReviewCount(0);
            }
            stadiumComplexRepository.save(complex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleBookingResponse> getEligibleBookingsForReview(
            Integer stadiumId, String userEmail) {
        log.info("Checking eligible bookings for review: stadiumId={}, user={}", stadiumId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Booking> eligibleBookings =
                bookingRepository.findCompletedUnreviewedBookings(user.getUserId(), stadiumId);

        return eligibleBookings.stream()
                .map(b -> EligibleBookingResponse.builder()
                        .bookingId(b.getBookingId())
                        .stadiumId(b.getStadium().getStadiumId())
                        .stadiumName(b.getStadium().getStadiumName())
                        .reservationDate(b.getReservationDate())
                        .slotStartTime(b.getSlot().getStartTime())
                        .slotEndTime(b.getSlot().getEndTime())
                        .bookingDate(b.getBookingDate())
                        .build())
                .toList();
    }
}
