package com.sportvenue.service.impl;

import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.StadiumComplexImage;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.entity.enums.StadiumStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicComplexServiceImplTest {

    @Mock
    private StadiumComplexRepository stadiumComplexRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private com.sportvenue.service.MaintenanceScheduleService maintenanceScheduleService;

    @InjectMocks
    private PublicComplexServiceImpl publicComplexService;

    @Test
    void getPublicComplexById_ShouldReturnResponse_WhenApprovedAndAvailable() {
        // Arrange
        Integer complexId = 1;
        User user = new User();
        user.setFirstName("Huy");
        user.setLastName("Nguyen");
        user.setPhoneNumber("0987654321");

        Owner owner = new Owner();
        owner.setUser(user);
        owner.setBusinessName("Kỳ Hòa Sports");

        StadiumComplex complex = StadiumComplex.builder()
                .complexId(complexId)
                .name("Kỳ Hòa")
                .address("Hồ Chí Minh")
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .averageRating(BigDecimal.valueOf(4.8))
                .reviewCount(10)
                .owner(owner)
                .sportTypes(Set.of(new SportType(1, "Football", "en", "FB", "desc", true, true, null)))
                .amenities(Set.of(new Amenity(1, "Parking", "parking-icon")))
                .images(Set.of(new StadiumComplexImage(1, null, "img-url", null)))
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(complexId)).thenReturn(Optional.of(complex));

        // Act
        PublicComplexDetailResponse response = publicComplexService.getPublicComplexById(complexId);

        // Assert
        assertNotNull(response);
        assertEquals("Kỳ Hòa", response.getName());
        assertEquals("Kỳ Hòa Sports", response.getOwnerName());
        assertEquals("0987654321", response.getOwnerPhone());
        assertEquals(1, response.getSportTypes().size());
        assertEquals("Football", response.getSportTypes().get(0).getSportName());
        assertEquals(1, response.getAmenities().size());
        assertEquals("Parking", response.getAmenities().get(0).getName());
        assertEquals(1, response.getImages().size());
    }

    @Test
    void getPublicComplexById_ShouldThrowException_WhenNotApproved() {
        // Arrange
        Integer complexId = 1;
        StadiumComplex complex = StadiumComplex.builder()
                .complexId(complexId)
                .approvedStatus(ApprovedStatus.PENDING)
                .complexStatus(ComplexStatus.AVAILABLE)
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(complexId)).thenReturn(Optional.of(complex));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> publicComplexService.getPublicComplexById(complexId));
    }

    @Test
    void getPublicComplexById_ShouldThrowException_WhenClosed() {
        // Arrange
        Integer complexId = 1;
        StadiumComplex complex = StadiumComplex.builder()
                .complexId(complexId)
                .approvedStatus(ApprovedStatus.APPROVED)
                .complexStatus(ComplexStatus.CLOSED)
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(complexId)).thenReturn(Optional.of(complex));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> publicComplexService.getPublicComplexById(complexId));
    }

    @Test
    void getFacilitiesByComplexId_ShouldReturnList_WhenComplexApproved() {
        // Arrange
        Integer complexId = 1;
        StadiumComplex complex = StadiumComplex.builder()
                .complexId(complexId)
                .approvedStatus(ApprovedStatus.APPROVED)
                .complexStatus(ComplexStatus.AVAILABLE)
                .build();

        SportType sportType = new SportType(1, "Football", "en", "FB", "desc", true, true, null);
        Stadium facility = Stadium.builder()
                .stadiumId(10)
                .stadiumName("Khu Sân Bóng Đá")
                .nodeType(StadiumNodeType.FACILITY)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .sportType(sportType)
                .images(Collections.emptySet())
                .build();

        when(stadiumComplexRepository.findById(complexId)).thenReturn(Optional.of(complex));
        when(stadiumRepository.findFacilitiesByComplexId(complexId)).thenReturn(List.of(facility));

        // Act
        List<FacilityResponse> response = publicComplexService.getFacilitiesByComplexId(complexId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Khu Sân Bóng Đá", response.get(0).getStadiumName());
        assertEquals("Football", response.get(0).getSportType().getSportName());
    }

    @Test
    void getCourtsByFacilityId_ShouldReturnList() {
        // Arrange
        Integer facilityId = 10;
        Stadium facility = Stadium.builder()
                .stadiumId(facilityId)
                .nodeType(StadiumNodeType.FACILITY)
                .build();

        Stadium court = Stadium.builder()
                .stadiumId(20)
                .stadiumName("Sân Bóng 1")
                .nodeType(StadiumNodeType.COURT)
                .parentStadium(facility)
                .stadiumStatus(StadiumStatus.AVAILABLE)
                .pricePerHour(BigDecimal.valueOf(150_000))
                .images(Collections.emptySet())
                .build();

        when(stadiumRepository.findById(facilityId)).thenReturn(Optional.of(facility));
        when(stadiumRepository.findCourtsByFacilityId(facilityId)).thenReturn(List.of(court));

        // Act
        List<CourtResponse> response = publicComplexService.getCourtsByFacilityId(facilityId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Sân Bóng 1", response.get(0).getStadiumName());
        assertEquals(facilityId, response.get(0).getParentStadiumId());
    }

    @Test
    void getCourtsByFacilityId_ShouldThrowBadRequestException_WhenNodeIsNotFacility() {
        // Arrange
        Integer facilityId = 10;
        Stadium notFacility = Stadium.builder()
                .stadiumId(facilityId)
                .nodeType(StadiumNodeType.COURT) // Not a FACILITY
                .build();

        when(stadiumRepository.findById(facilityId)).thenReturn(Optional.of(notFacility));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> publicComplexService.getCourtsByFacilityId(facilityId));
    }
}
