package com.sportvenue.controller;

import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.service.PublicComplexService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicComplexControllerTest {

    @Mock
    private PublicComplexService publicComplexService;

    @InjectMocks
    private PublicComplexController publicComplexController;

    @InjectMocks
    private PublicFacilityController publicFacilityController;

    @Test
    void getPublicComplexById_Success() {
        // Arrange
        Integer id = 1;
        PublicComplexDetailResponse response = PublicComplexDetailResponse.builder()
                .complexId(id)
                .name("Kỳ Hòa Sports")
                .build();
        when(publicComplexService.getPublicComplexById(id)).thenReturn(response);

        // Act
        ResponseEntity<PublicComplexDetailResponse> result = publicComplexController.getPublicComplexById(id);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Kỳ Hòa Sports", result.getBody().getName());
        verify(publicComplexService).getPublicComplexById(id);
    }

    @Test
    void getFacilitiesByComplexId_Success() {
        // Arrange
        Integer id = 1;
        List<FacilityResponse> response = List.of(
                FacilityResponse.builder().stadiumId(10).stadiumName("Khu Sân Bóng").build()
        );
        when(publicComplexService.getFacilitiesByComplexId(id)).thenReturn(response);

        // Act
        ResponseEntity<List<FacilityResponse>> result = publicComplexController.getFacilitiesByComplexId(id);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("Khu Sân Bóng", result.getBody().get(0).getStadiumName());
        verify(publicComplexService).getFacilitiesByComplexId(id);
    }

    @Test
    void getCourtsByFacilityId_Success() {
        // Arrange
        Integer id = 10;
        List<CourtResponse> response = List.of(
                CourtResponse.builder().stadiumId(20).stadiumName("Sân Bóng 1").build()
        );
        when(publicComplexService.getCourtsByFacilityId(id)).thenReturn(response);

        // Act
        ResponseEntity<List<CourtResponse>> result = publicFacilityController.getCourtsByFacilityId(id);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("Sân Bóng 1", result.getBody().get(0).getStadiumName());
        verify(publicComplexService).getCourtsByFacilityId(id);
    }
}
