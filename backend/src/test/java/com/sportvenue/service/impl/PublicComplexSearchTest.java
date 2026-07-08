package com.sportvenue.service.impl;

import com.sportvenue.dto.request.StadiumComplexSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicComplexSearchTest {

    @Mock
    private StadiumComplexRepository stadiumComplexRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @InjectMocks
    private PublicComplexServiceImpl publicComplexService;

    private StadiumComplex complex1;
    private StadiumComplex complex2;

    @BeforeEach
    void setUp() {
        User user = User.builder().firstName("Test").lastName("User").phoneNumber("0123456789").build();
        Owner owner = Owner.builder().user(user).businessName("Owner Biz").build();

        complex1 = StadiumComplex.builder()
                .complexId(1)
                .name("Complex A")
                .address("HCMC")
                .latitude(10.7769) // Distance 0
                .longitude(106.7009)
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .owner(owner)
                .build();

        complex2 = StadiumComplex.builder()
                .complexId(2)
                .name("Complex B")
                .address("HCMC")
                .latitude(10.8231) // Distance ~6km away
                .longitude(106.6297)
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .owner(owner)
                .build();
    }

    @Test
    void searchComplexes_WithoutLocation_ShouldReturnPagedResponse() {
        // Arrange
        StadiumComplexSearchRequest request = StadiumComplexSearchRequest.builder()
                .keyword("Complex")
                .page(0)
                .size(10)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<StadiumComplex> page = new PageImpl<>(List.of(complex1, complex2), pageable, 2);

        when(stadiumComplexRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        
        // Mock the single pricing query response
        Object[] row1 = new Object[]{1, BigDecimal.valueOf(100000.0), BigDecimal.valueOf(150000.0)};
        Object[] row2 = new Object[]{2, BigDecimal.valueOf(200000.0), BigDecimal.valueOf(250000.0)};
        when(stadiumRepository.findMinMaxPriceByComplexIds(List.of(1, 2))).thenReturn(List.of(row1, row2));

        // Act
        PageResponse<PublicComplexDetailResponse> response = publicComplexService.searchComplexes(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals("Complex A", response.getContent().get(0).getName());
        assertEquals(BigDecimal.valueOf(100000.0), response.getContent().get(0).getMinPrice());
        assertEquals(BigDecimal.valueOf(150000.0), response.getContent().get(0).getMaxPrice());

        assertEquals("Complex B", response.getContent().get(1).getName());
        assertEquals(BigDecimal.valueOf(200000.0), response.getContent().get(1).getMinPrice());
        assertEquals(BigDecimal.valueOf(250000.0), response.getContent().get(1).getMaxPrice());

        verify(stadiumComplexRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(stadiumRepository).findMinMaxPriceByComplexIds(List.of(1, 2));
    }

    @Test
    void searchComplexes_WithLocation_ShouldSortByHaversineDistance() {
        // Arrange
        StadiumComplexSearchRequest request = StadiumComplexSearchRequest.builder()
                .userLat(10.7769)
                .userLng(106.7009)
                .radiusInKm(15.0)
                .page(0)
                .size(10)
                .build();

        // Database returns them unsorted (Complex 2 first, then Complex 1)
        when(stadiumComplexRepository.findAll(any(Specification.class))).thenReturn(List.of(complex2, complex1));
        
        Object[] row1 = new Object[]{1, BigDecimal.valueOf(100000.0), BigDecimal.valueOf(150000.0)};
        Object[] row2 = new Object[]{2, BigDecimal.valueOf(200000.0), BigDecimal.valueOf(250000.0)};
        when(stadiumRepository.findMinMaxPriceByComplexIds(any())).thenReturn(List.of(row1, row2));

        // Act
        PageResponse<PublicComplexDetailResponse> response = publicComplexService.searchComplexes(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        
        // Complex A (complex1) must be first (distance = 0.0)
        assertEquals("Complex A", response.getContent().get(0).getName());
        assertEquals(0.0, response.getContent().get(0).getDistanceInKm(), 0.1);

        // Complex B (complex2) must be second (distance > 0)
        assertEquals("Complex B", response.getContent().get(1).getName());
        assertTrue(response.getContent().get(1).getDistanceInKm() > 5.0);

        verify(stadiumComplexRepository).findAll(any(Specification.class));
    }

    @Test
    void searchComplexes_EmptyResult_ShouldReturnEmptyPage() {
        // Arrange
        StadiumComplexSearchRequest request = StadiumComplexSearchRequest.builder()
                .keyword("Nonexistent")
                .page(0)
                .size(10)
                .build();

        when(stadiumComplexRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        PageResponse<PublicComplexDetailResponse> response = publicComplexService.searchComplexes(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
        verify(stadiumRepository, never()).findMinMaxPriceByComplexIds(any());
    }
}
