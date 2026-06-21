package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateReviewRequest;
import com.sportvenue.dto.response.EligibleBookingResponse;
import com.sportvenue.dto.response.ReviewResponse;
import com.sportvenue.entity.Booking;
import com.sportvenue.entity.Review;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.BookingStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.BookingRepository;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReviewServiceImpl reviewService;

    private User buildUser(Integer id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private Stadium buildStadium(Integer id, String name) {
        Stadium stadium = new Stadium();
        stadium.setStadiumId(id);
        stadium.setStadiumName(name);
        stadium.setAverageRating(BigDecimal.valueOf(5.0));
        stadium.setReviewCount(0);
        return stadium;
    }

    private Booking buildBooking(Integer id, User user, Stadium stadium, BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingId(id);
        booking.setUser(user);
        booking.setStadium(stadium);
        booking.setBookingStatus(status);
        booking.setReservationDate(LocalDate.now().minusDays(7));
        return booking;
    }

    // ─── createReview ─────────────────────────────────────────────────────────

    @Test
    void createReview_Success_UpdatesRatingAndCount() {
        User user = buildUser(1, "customer@sportvenue.com");
        Stadium stadium = buildStadium(1, "Sân Test");
        Booking booking = buildBooking(1, user, stadium, BookingStatus.COMPLETED);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setRatingScore(4);
        request.setComment("Sân sạch sẽ, mặt cỏ tốt!");

        Review savedReview = Review.builder()
                .reviewId(1).booking(booking).user(user).stadium(stadium)
                .ratingScore(4).comment("Sân sạch sẽ, mặt cỏ tốt!")
                .build();

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingBookingId(1)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.calculateAverageRating(1)).thenReturn(Optional.of(4.0));
        when(reviewRepository.countByStadiumStadiumId(1)).thenReturn(3L);

        ReviewResponse result = reviewService.createReview(1, request, "customer@sportvenue.com");

        assertNotNull(result);
        assertEquals(4, result.getRatingScore());
        assertEquals(BigDecimal.valueOf(4.0), stadium.getAverageRating());
        assertEquals(3, stadium.getReviewCount());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("ghost@sportvenue.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(1, new CreateReviewRequest(), "ghost@sportvenue.com"));
    }

    @Test
    void createReview_BookingNotFound_ThrowsException() {
        User user = buildUser(1, "customer@sportvenue.com");
        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(999, new CreateReviewRequest(), "customer@sportvenue.com"));
    }

    @Test
    void createReview_WrongUser_ThrowsException() {
        User user = buildUser(1, "customer@sportvenue.com");
        User otherUser = buildUser(2, "other@sportvenue.com");
        Stadium stadium = buildStadium(1, "Sân Test");
        Booking booking = buildBooking(1, otherUser, stadium, BookingStatus.COMPLETED);

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1, new CreateReviewRequest(), "customer@sportvenue.com"));
    }

    @Test
    void createReview_BookingNotCompleted_ThrowsException() {
        User user = buildUser(1, "customer@sportvenue.com");
        Stadium stadium = buildStadium(1, "Sân Test");
        Booking booking = buildBooking(1, user, stadium, BookingStatus.PENDING);

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1, new CreateReviewRequest(), "customer@sportvenue.com"));
    }

    @Test
    void createReview_AlreadyReviewed_ThrowsException() {
        User user = buildUser(1, "customer@sportvenue.com");
        Stadium stadium = buildStadium(1, "Sân Test");
        Booking booking = buildBooking(1, user, stadium, BookingStatus.COMPLETED);

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findById(1)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingBookingId(1)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(1, new CreateReviewRequest(), "customer@sportvenue.com"));
    }

    // ─── getEligibleBookingsForReview ─────────────────────────────────────────

    @Test
    void getEligibleBookingsForReview_Success_ReturnsList() {
        User user = buildUser(1, "customer@sportvenue.com");
        Stadium stadium = buildStadium(1, "Sân Test");

        TimeSlot slot = new TimeSlot();
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(9, 0));

        Booking booking = buildBooking(1, user, stadium, BookingStatus.COMPLETED);
        booking.setSlot(slot);

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findCompletedUnreviewedBookings(1, 1)).thenReturn(List.of(booking));

        List<EligibleBookingResponse> result =
                reviewService.getEligibleBookingsForReview(1, "customer@sportvenue.com");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getBookingId());
        assertEquals("Sân Test", result.get(0).getStadiumName());
        assertEquals(LocalTime.of(8, 0), result.get(0).getSlotStartTime());
    }

    @Test
    void getEligibleBookingsForReview_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("ghost@sportvenue.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.getEligibleBookingsForReview(1, "ghost@sportvenue.com"));
    }

    @Test
    void getEligibleBookingsForReview_NoEligibleBookings_ReturnsEmpty() {
        User user = buildUser(1, "customer@sportvenue.com");

        when(userRepository.findByEmail("customer@sportvenue.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findCompletedUnreviewedBookings(1, 1)).thenReturn(List.of());

        List<EligibleBookingResponse> result =
                reviewService.getEligibleBookingsForReview(1, "customer@sportvenue.com");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
