package com.sportvenue.service;

import com.sportvenue.dto.request.VenueReviewRequest;
import com.sportvenue.dto.response.VenueReviewResponse;
import com.sportvenue.entity.Role;
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
import com.sportvenue.service.impl.VenueReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueReviewServiceImplTest {

    @Mock
    private VenueReviewRepository venueReviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private VenueReviewServiceImpl venueReviewService;

    private User mockUser;
    private Stadium mockStadium;
    private VenueReviewRequest validRequest;

    @BeforeEach
    void setUp() {
        Role customerRole = Role.builder().roleId(2).roleName("CUSTOMER").build();
        mockUser = User.builder()
                .userId(1)
                .email("customer@test.com")
                .role(customerRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        mockStadium = Stadium.builder()
                .stadiumId(10)
                .stadiumName("Test Stadium")
                .reviewCount(0)
                .averageRating(BigDecimal.valueOf(5.0))
                .build();

        validRequest = VenueReviewRequest.builder()
                .rating(5)
                .comment("Great place!")
                .build();
    }

    @Test
    void createReview_Success() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(mockStadium));
        when(bookingRepository.existsByUserUserIdAndStadiumStadiumIdAndBookingStatus(1, 10, BookingStatus.COMPLETED))
                .thenReturn(true);
        when(venueReviewRepository.existsByCustomer_UserIdAndVenue_StadiumId(1, 10)).thenReturn(false);
        
        VenueReview savedReview = VenueReview.builder()
                .id(100)
                .venue(mockStadium)
                .customer(mockUser)
                .rating(5)
                .comment("Great place!")
                .build();
        when(venueReviewRepository.save(any(VenueReview.class))).thenReturn(savedReview);

        // Act
        VenueReviewResponse response = venueReviewService.createReview(10, validRequest, "customer@test.com");

        // Assert
        assertNotNull(response);
        assertEquals(100, response.getId());
        assertEquals(10, response.getVenueId());
        assertEquals(1, response.getCustomerId());
        assertEquals(5, response.getRating());
        
        verify(stadiumRepository).save(mockStadium);
        assertEquals(1, mockStadium.getReviewCount());
        assertEquals(BigDecimal.valueOf(5.00).setScale(2), mockStadium.getAverageRating());
    }

    @Test
    void createReview_FailsWhenNotCustomer() {
        // Arrange
        mockUser.getRole().setRoleName("OWNER");
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, 
                () -> venueReviewService.createReview(10, validRequest, "customer@test.com"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void createReview_FailsWhenAccountDisabled() {
        // Arrange
        mockUser.setAccountStatus(AccountStatus.BLOCKED);
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AppException ex = assertThrows(AppException.class, 
                () -> venueReviewService.createReview(10, validRequest, "customer@test.com"));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void createReview_FailsWhenVenueNotFound() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));
        when(stadiumRepository.findById(10)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, 
                () -> venueReviewService.createReview(10, validRequest, "customer@test.com"));
        assertEquals("Venue not found", ex.getMessage());
    }

    @Test
    void createReview_FailsWhenNoCompletedBooking() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(mockStadium));
        when(bookingRepository.existsByUserUserIdAndStadiumStadiumIdAndBookingStatus(1, 10, BookingStatus.COMPLETED))
                .thenReturn(false);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> venueReviewService.createReview(10, validRequest, "customer@test.com"));
        assertEquals("Customer has no completed booking for this venue", ex.getMessage());
    }

    @Test
    void createReview_FailsWhenAlreadyReviewed() {
        // Arrange
        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(mockStadium));
        when(bookingRepository.existsByUserUserIdAndStadiumStadiumIdAndBookingStatus(1, 10, BookingStatus.COMPLETED))
                .thenReturn(true);
        when(venueReviewRepository.existsByCustomer_UserIdAndVenue_StadiumId(1, 10)).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, 
                () -> venueReviewService.createReview(10, validRequest, "customer@test.com"));
        assertEquals("Review already exists for this venue", ex.getMessage());
    }
    
    @Test
    void createReview_UpdatesAverageRatingCorrectly() {
        // Arrange
        mockStadium.setReviewCount(2);
        mockStadium.setAverageRating(BigDecimal.valueOf(4.0)); // Total score = 8
        
        validRequest.setRating(1); // New total = 9, count = 3, avg = 3.0

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(mockUser));
        when(stadiumRepository.findById(10)).thenReturn(Optional.of(mockStadium));
        when(bookingRepository.existsByUserUserIdAndStadiumStadiumIdAndBookingStatus(1, 10, BookingStatus.COMPLETED))
                .thenReturn(true);
        when(venueReviewRepository.existsByCustomer_UserIdAndVenue_StadiumId(1, 10)).thenReturn(false);
        
        VenueReview savedReview = VenueReview.builder()
                .id(100)
                .venue(mockStadium)
                .customer(mockUser)
                .rating(1)
                .build();
        when(venueReviewRepository.save(any(VenueReview.class))).thenReturn(savedReview);

        // Act
        venueReviewService.createReview(10, validRequest, "customer@test.com");

        // Assert
        assertEquals(3, mockStadium.getReviewCount());
        assertEquals(BigDecimal.valueOf(3.00).setScale(2), mockStadium.getAverageRating());
    }
}
