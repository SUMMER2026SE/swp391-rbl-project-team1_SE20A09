package com.sportvenue.service.impl;

import com.sportvenue.dto.request.VenueReviewRequest;
import com.sportvenue.dto.response.VenueReviewResponse;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.User;
import com.sportvenue.entity.VenueReview;
import com.sportvenue.entity.enums.AccountStatus;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.ErrorCode;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.repository.VenueReviewRepository;
import com.sportvenue.service.VenueReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueReviewServiceImpl implements VenueReviewService {

    private final VenueReviewRepository venueReviewRepository;
    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public VenueReviewResponse createReview(Integer venueId, VenueReviewRequest request, String username) {
        log.info("Creating venue review for venueId: {} by user: {}", venueId, username);

        // 1. Get user and validate status
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getRole().getRoleName().equalsIgnoreCase("CUSTOMER")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. Get venue
        Stadium venue = stadiumRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found"));

        // 3. Check if customer has a COMPLETED booking for this venue
        boolean hasCompletedBooking = bookingRepository.existsByUserUserIdAndStadiumStadiumIdAndBookingStatus(
                user.getUserId(), venueId, BookingStatus.COMPLETED);

        if (!hasCompletedBooking) {
            throw new BadRequestException("Customer has no completed booking for this venue");
        }

        // 4. Check if review already exists
        boolean alreadyReviewed = venueReviewRepository.existsByCustomer_UserIdAndVenue_StadiumId(user.getUserId(), venueId);
        if (alreadyReviewed) {
            throw new BadRequestException("Review already exists for this venue");
        }

        // 5. Calculate new rating and review count
        int oldCount = venue.getReviewCount();
        BigDecimal oldAvg = venue.getAverageRating();

        int newCount = oldCount + 1;
        
        // (oldAvg * oldCount + rating) / newCount
        BigDecimal totalScore = oldAvg.multiply(BigDecimal.valueOf(oldCount)).add(BigDecimal.valueOf(request.getRating()));
        BigDecimal newAvg = totalScore.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        venue.setReviewCount(newCount);
        venue.setAverageRating(newAvg);
        stadiumRepository.save(venue);

        // 6. Save review
        VenueReview review = VenueReview.builder()
                .venue(venue)
                .customer(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        VenueReview savedReview = venueReviewRepository.save(review);

        // 7. Map to response
        return VenueReviewResponse.builder()
                .id(savedReview.getId())
                .venueId(savedReview.getVenue().getStadiumId())
                .customerId(savedReview.getCustomer().getUserId())
                .rating(savedReview.getRating())
                .comment(savedReview.getComment())
                .createdAt(savedReview.getCreatedAt())
                .updatedAt(savedReview.getUpdatedAt())
                .build();
    }
}
