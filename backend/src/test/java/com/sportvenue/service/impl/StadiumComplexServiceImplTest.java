package com.sportvenue.service.impl;

import com.sportvenue.dto.request.CreateComplexRequest;
import com.sportvenue.dto.response.ComplexResponse;
import com.sportvenue.entity.Amenity;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.SportType;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.ApprovedStatus;
import com.sportvenue.entity.enums.ComplexStatus;
import com.sportvenue.exception.AppException;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.repository.AmenityRepository;
import com.sportvenue.repository.OwnerRepository;
import com.sportvenue.repository.SportTypeRepository;
import com.sportvenue.repository.StadiumComplexImageRepository;
import com.sportvenue.repository.StadiumComplexRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StadiumComplexServiceImplTest {

    @Mock
    private StadiumComplexRepository stadiumComplexRepository;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private SportTypeRepository sportTypeRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private StadiumComplexImageRepository stadiumComplexImageRepository;

    private StadiumComplexServiceImpl stadiumComplexService;

    private Owner owner;
    private User user;

    @BeforeEach
    void setUp() {
        stadiumComplexService = new StadiumComplexServiceImpl(
                stadiumComplexRepository,
                ownerRepository,
                sportTypeRepository,
                amenityRepository,
                stadiumComplexImageRepository
        );

        user = User.builder().userId(1).email("owner@sportvenue.com").build();
        owner = Owner.builder().ownerId(10).user(user).approvedStatus(ApprovedStatus.APPROVED).build();
    }

    @Test
    void createComplex_Success() {
        CreateComplexRequest request = CreateComplexRequest.builder()
                .name("Tổ hợp Hoa Lư")
                .address("Quận 1")
                .phone("0909090909")
                .latitude(BigDecimal.valueOf(10.77))
                .longitude(BigDecimal.valueOf(106.69))
                .sportTypeIds(Set.of(1))
                .amenityIds(Set.of(2))
                .imageUrls(List.of("http://image1.jpg"))
                .build();

        SportType sportType = SportType.builder().sportTypeId(1).sportName("Football").build();
        Amenity amenity = Amenity.builder().amenityId(2).name("Parking").build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(sportTypeRepository.findAllById(any())).thenReturn(List.of(sportType));
        when(amenityRepository.findAllById(any())).thenReturn(List.of(amenity));

        when(stadiumComplexRepository.save(any(StadiumComplex.class))).thenAnswer(invocation -> {
            StadiumComplex c = invocation.getArgument(0);
            c.setComplexId(100);
            return c;
        });

        ComplexResponse response = stadiumComplexService.createComplex(request, 1);

        assertNotNull(response);
        assertEquals(100, response.getComplexId());
        assertEquals("Tổ hợp Hoa Lư", response.getName());
        assertEquals("PENDING", response.getApprovedStatus());
        verify(stadiumComplexImageRepository).saveAll(any());
    }

    @Test
    void createComplex_OwnerNotApproved_ThrowsException() {
        owner.setApprovedStatus(ApprovedStatus.PENDING);
        CreateComplexRequest request = CreateComplexRequest.builder().name("Tổ hợp Hoa Lư").address("Quận 1").build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));

        assertThrows(AppException.class, () -> stadiumComplexService.createComplex(request, 1));
    }

    @Test
    void getMyComplexes_Success() {
        StadiumComplex c = StadiumComplex.builder()
                .complexId(100)
                .name("Tổ hợp Hoa Lư")
                .owner(owner)
                .complexStatus(ComplexStatus.AVAILABLE)
                .approvedStatus(ApprovedStatus.APPROVED)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumComplexRepository.findByOwnerOwnerId(10)).thenReturn(List.of(c));

        List<ComplexResponse> list = stadiumComplexService.getMyComplexes(1);
        assertEquals(1, list.size());
        assertEquals(100, list.get(0).getComplexId());
    }

    @Test
    void getComplexByIdAndOwner_NotOwn_ThrowsException() {
        Owner otherOwner = Owner.builder().ownerId(20).build();
        StadiumComplex c = StadiumComplex.builder()
                .complexId(100)
                .owner(otherOwner)
                .build();

        when(ownerRepository.findByUserUserId(1)).thenReturn(Optional.of(owner));
        when(stadiumComplexRepository.findById(100)).thenReturn(Optional.of(c));

        assertThrows(BadRequestException.class, () -> stadiumComplexService.getComplexByIdAndOwner(100, 1));
    }

    @Test
    void approveComplex_Success() {
        StadiumComplex c = StadiumComplex.builder()
                .complexId(100)
                .owner(owner)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        when(stadiumComplexRepository.findById(100)).thenReturn(Optional.of(c));
        when(stadiumComplexRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComplexResponse response = stadiumComplexService.approveComplex(100);
        assertEquals("APPROVED", response.getApprovedStatus());
    }

    @Test
    void rejectComplex_Success() {
        StadiumComplex c = StadiumComplex.builder()
                .complexId(100)
                .owner(owner)
                .approvedStatus(ApprovedStatus.PENDING)
                .build();

        when(stadiumComplexRepository.findById(100)).thenReturn(Optional.of(c));
        when(stadiumComplexRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComplexResponse response = stadiumComplexService.rejectComplex(100, "Không hợp lệ");
        assertEquals("REJECTED", response.getApprovedStatus());
        assertEquals("Không hợp lệ", response.getRejectionReason());
    }
}
