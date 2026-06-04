package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Notification;
import com.sportvenue.entity.enums.NotificationType;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.User;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.NotificationRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.UserRepository;
import com.sportvenue.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getOwnerReviews(String ownerEmail) {
        log.info("Fetching reviews for owner: {}", ownerEmail);
        List<Review> reviews = reviewRepository.findByStadiumOwnerUserEmailOrderByCreatedAtDesc(ownerEmail);
        return reviews.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ReviewResponse> getCustomerReviews(String customerEmail) {
        log.info("Fetching reviews for customer: {}", customerEmail);
        List<Review> reviews = reviewRepository.findByUserEmailOrderByCreatedAtDesc(customerEmail);
        return reviews.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ReviewResponse replyToReview(Integer reviewId, String ownerResponse, String ownerEmail) {
        log.info("Owner {} is replying to reviewId: {}", ownerEmail, reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá ID: " + reviewId));

        User user = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng: " + ownerEmail));

        Owner owner = ownerRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không phải là chủ sân (Owner)"));

        // Ensure owner owns the stadium that the review is for
        if (!review.getStadium().getOwner().getOwnerId().equals(owner.getOwnerId())) {
            throw new BadRequestException("Bạn không có quyền phản hồi đánh giá của sân này!");
        }

        review.setOwnerResponse(ownerResponse.trim());
        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Transactional
    @Override
    public ReviewResponse createReview(CreateReviewRequest request, String customerEmail) {
        log.info("Customer {} is creating review for bookingId: {}", customerEmail, request.getBookingId());

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt sân"));

        if (!booking.getUser().getUserId().equals(customer.getUserId())) {
            throw new BadRequestException("Bạn không thể đánh giá đơn đặt sân của người khác");
        }

        if (reviewRepository.existsByBookingBookingId(booking.getBookingId())) {
            throw new BadRequestException("Bạn đã đánh giá đơn đặt sân này rồi");
        }

        Review review = Review.builder()
                .booking(booking)
                .user(customer)
                .stadium(booking.getStadium())
                .ratingScore(request.getRating())
                .comment(request.getComment().trim())
                .createdAt(java.time.LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);

        // Gửi thông báo cho chủ sân
        User ownerUser = booking.getStadium().getOwner().getUser();
        Notification notification = Notification.builder()
                .user(ownerUser)
                .notificationType(NotificationType.REVIEW)
                .title("Đánh giá mới từ khách hàng ⭐")
                .message("Khách hàng " + customer.getFirstName() + " đã đánh giá " + request.getRating() + " sao cho sân \"" + booking.getStadium().getStadiumName() + "\".")
                .relatedResourceId(String.valueOf(saved.getReviewId()))
                .build();
        notificationRepository.save(notification);

        return mapToResponse(saved);
    }

    private ReviewResponse mapToResponse(Review r) {
        String customerFullName = r.getUser().getFirstName() + " " + r.getUser().getLastName();
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .id("REV-" + r.getReviewId())
                .bookingId(r.getBooking().getBookingId())
                .stadiumName(r.getStadium().getStadiumName())
                .venueName(r.getStadium().getStadiumName())
                .customerName(customerFullName)
                .rating(r.getRatingScore())
                .comment(r.getComment())
                .tags(new ArrayList<>()) // Reviews table doesn't store tags, return empty list
                .createdAt(r.getCreatedAt().format(ISO_FORMATTER))
                .ownerResponse(r.getOwnerResponse())
                .build();
    }
}
