package com.sportvenue.service.impl;

import com.sportvenue.dto.request.StadiumSearchRequest;
import com.sportvenue.dto.response.PageResponse;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PublicStadiumServiceImplTest {

    @Mock
    private StadiumRepository stadiumRepository;

    @InjectMocks
    private PublicStadiumServiceImpl publicStadiumService;

    @Test
    void searchStadiums_WithLocation_ShouldCalculateDistanceAndSort() {
        // Arrange
        StadiumSearchRequest request = new StadiumSearchRequest();
        request.setPage(0);
        request.setSize(10);
        request.setUserLat(10.7769); // HCMC
        request.setUserLng(106.7009);
        request.setRadiusInKm(10.0);

        SportType sportType = new SportType();
        sportType.setSportName("Football");

        Stadium stadium1 = new Stadium();
        stadium1.setStadiumId(1);
        stadium1.setLatitude(10.7769); // 0 km
        stadium1.setLongitude(106.7009);
        stadium1.setSportType(sportType);
        stadium1.setAmenities(Collections.emptySet());

        Stadium stadium2 = new Stadium();
        stadium2.setStadiumId(2);
        stadium2.setLatitude(10.8231); // ~5-6 km away
        stadium2.setLongitude(106.6297);
        stadium2.setSportType(sportType);
        stadium2.setAmenities(Collections.emptySet());

        Page<Stadium> page = new PageImpl<>(List.of(stadium2, stadium1), PageRequest.of(0, 10), 2);

        when(stadiumRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);
        when(stadiumRepository.findAllByStadiumIdIn(any(List.class)))
                .thenReturn(List.of(stadium1, stadium2));

        // Act
        PageResponse<StadiumResponse> response = publicStadiumService.searchStadiums(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());

        // Stadium 1 should be first because distance is 0
        assertEquals(1, response.getContent().get(0).getStadiumId());
        assertEquals(0.0, response.getContent().get(0).getDistanceInKm(), 0.1);

        // Stadium 2 should be second
        assertEquals(2, response.getContent().get(1).getStadiumId());
        assertTrue(response.getContent().get(1).getDistanceInKm() > 0.0);
    }
}
