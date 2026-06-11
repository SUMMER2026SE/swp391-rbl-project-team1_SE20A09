package com.sportvenue.service;

import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumDetailResponse;
import com.sportvenue.entity.*;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.ReviewRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.service.impl.PublicStadiumServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStadiumServiceImplTest {

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private PublicStadiumServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PublicStadiumServiceImpl(stadiumRepository, reviewRepository);
    }

    // ── Helper builders ──────────────────────────────────────────────────────

    private Stadium buildStadium(int id) {
        SportType sport = new SportType();
        sport.setSportTypeId(1);
        sport.setSportName("Football");

        User ownerUser = new User();
        ownerUser.setFirstName("Owner");
        ownerUser.setLastName("Name");
        ownerUser.setPhoneNumber("0900000001");

        Owner owner = new Owner();
        owner.setOwnerId(1);
        owner.setUser(ownerUser);

        Stadium s = Stadium.builder()
                .stadiumId(id)
                .stadiumName("Test Stadium")
                .address("123 Test St")
                .pricePerHour(BigDecimal.valueOf(100_000))
                .capacity(10)
                .averageRating(BigDecimal.valueOf(4.5))
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(22, 0))
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .sportType(sport)
                .owner(owner)
                .images(new LinkedHashSet<>())
                .amenities(new java.util.HashSet<>())
                .accessories(new LinkedHashSet<>())
                .timeSlots(new LinkedHashSet<>())
                .build();
        return s;
    }

    // ── getStadiumDetail ─────────────────────────────────────────────────────

    @Test
    void getStadiumDetail_success_returnsFullResponse() {
        Stadium stadium = buildStadium(1);
        when(stadiumRepository.findWithDetailsByStadiumId(1)).thenReturn(Optional.of(stadium));
        when(reviewRepository.findByStadiumStadiumIdOrderByCreatedAtDesc(eq(1), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(reviewRepository.countByStadiumStadiumId(1)).thenReturn(0L);

        StadiumDetailResponse response = service.getStadiumDetail(1);

        assertNotNull(response);
        assertEquals(1, response.getStadiumId());
        assertEquals("Test Stadium", response.getStadiumName());
        assertEquals("Football", response.getSportName());
        assertEquals(0L, response.getTotalReviews());
        assertNotNull(response.getOwner());
        assertEquals("Owner Name", response.getOwner().getOwnerName());
        assertTrue(response.getTimeSlots().isEmpty());
        assertTrue(response.getAmenities().isEmpty());
        assertTrue(response.getAccessories().isEmpty());
        assertTrue(response.getRecentReviews().isEmpty());
    }

    @Test
    void getStadiumDetail_notFound_throwsResourceNotFoundException() {
        when(stadiumRepository.findWithDetailsByStadiumId(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getStadiumDetail(999));
    }

    // ── getStadiumReviews ────────────────────────────────────────────────────

    @Test
    void getStadiumReviews_success_returnsPageResponse() {
        when(stadiumRepository.existsById(1)).thenReturn(true);
        when(reviewRepository.findByStadiumStadiumIdOrderByCreatedAtDesc(eq(1), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0));

        PageResponse<StadiumDetailResponse.ReviewDto> response =
                service.getStadiumReviews(1, 0, 5);

        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getPageNo());
        assertEquals(5, response.getPageSize());
        assertEquals(0L, response.getTotalElements());
    }

    @Test
    void getStadiumReviews_stadiumNotFound_throwsResourceNotFoundException() {
        when(stadiumRepository.existsById(999)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.getStadiumReviews(999, 0, 5));
    }

    @Test
    void getStadiumReviews_withReviews_mapsFieldsCorrectly() {
        User reviewer = new User();
        reviewer.setFirstName("Nguyen");
        reviewer.setLastName("Van A");
        reviewer.setAvatarUrl(null);

        Review review = new Review();
        review.setReviewId(1);
        review.setUser(reviewer);
        review.setRatingScore(5);
        review.setComment("Sân rất tốt!");
        review.setOwnerResponse(null);
        review.setCreatedAt(LocalDateTime.now());

        when(stadiumRepository.existsById(1)).thenReturn(true);
        when(reviewRepository.findByStadiumStadiumIdOrderByCreatedAtDesc(eq(1), any()))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 5), 1));

        PageResponse<StadiumDetailResponse.ReviewDto> response =
                service.getStadiumReviews(1, 0, 5);

        assertEquals(1, response.getContent().size());
        StadiumDetailResponse.ReviewDto dto = response.getContent().get(0);
        assertEquals("Nguyen Van A", dto.getUserName());
        assertEquals(5, dto.getRatingScore());
        assertEquals("Sân rất tốt!", dto.getComment());
    }
}
