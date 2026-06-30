package com.sportvenue.service;

import com.sportvenue.dto.request.BulkTimeSlotRequest;
import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumComplex;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.entity.enums.StadiumNodeType;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.TimeSlotMapper;
import com.sportvenue.repository.StadiumComplexRepository;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.impl.ComplexTimeSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplexTimeSlotServiceImplTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private StadiumComplexRepository stadiumComplexRepository;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @InjectMocks
    private ComplexTimeSlotServiceImpl complexTimeSlotService;

    private User ownerUser;
    private Owner owner;
    private StadiumComplex complex;
    private Stadium facility;
    private Stadium court1;
    private Stadium court2;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().userId(1).email("owner@test.com").build();
        owner = Owner.builder().ownerId(1).user(ownerUser).build();

        complex = StadiumComplex.builder()
                .complexId(10)
                .name("Tổ hợp Thể thao Tuyên Sơn")
                .owner(owner)
                .build();

        facility = Stadium.builder()
                .stadiumId(100)
                .stadiumName("Khu Sân Bóng Đá")
                .nodeType(StadiumNodeType.FACILITY)
                .complex(complex)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(22, 0))
                .build();

        court1 = Stadium.builder()
                .stadiumId(101)
                .stadiumName("Sân số 1")
                .nodeType(StadiumNodeType.COURT)
                .parentStadium(facility)
                .complex(complex)
                .build();

        court2 = Stadium.builder()
                .stadiumId(102)
                .stadiumName("Sân số 2")
                .nodeType(StadiumNodeType.COURT)
                .parentStadium(facility)
                .complex(complex)
                .build();
    }

    @Test
    void bulkCreateSlotsForFacility_Success() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(true)
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(6, 0))
                                .endTime(LocalTime.of(7, 0))
                                .pricePerSlot(BigDecimal.valueOf(150000))
                                .build()
                ))
                .build();

        when(stadiumRepository.findFacilityWithComplexDetails(100)).thenReturn(Optional.of(facility));
        when(stadiumRepository.findCourtsByFacilityId(100)).thenReturn(List.of(court1, court2));
        when(timeSlotRepository.findOverlappingSlots(any(), any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotMapper.toEntity(any())).thenReturn(new TimeSlot());
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlotResponse> responses = complexTimeSlotService.bulkCreateSlotsForFacility(100, request, ownerUser.getUserId());

        assertNotNull(responses);
        verify(timeSlotRepository).saveAll(anyList());
    }

    @Test
    void bulkCreateSlotsForFacility_OwnershipViolation_ThrowsException() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder().applyToAllCourts(true).build();
        when(stadiumRepository.findFacilityWithComplexDetails(100)).thenReturn(Optional.of(facility));

        // Gọi bằng userId = 99 (không phải owner của Complex là 1)
        assertThrows(BadRequestException.class, () ->
                complexTimeSlotService.bulkCreateSlotsForFacility(100, request, 99));
    }

    @Test
    void bulkCreateSlotsForFacility_OverlapRollback_ThrowsException() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(true)
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(9, 0))
                                .pricePerSlot(BigDecimal.valueOf(150000))
                                .build()
                ))
                .build();

        when(stadiumRepository.findFacilityWithComplexDetails(100)).thenReturn(Optional.of(facility));
        when(stadiumRepository.findCourtsByFacilityId(100)).thenReturn(List.of(court1, court2));

        // Mock court2 bị trùng giờ (overlap), nên transaction sẽ rollback
        when(timeSlotRepository.findOverlappingSlots(eq(101), any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotRepository.findOverlappingSlots(eq(102), any(), any())).thenReturn(List.of(new TimeSlot()));
        when(timeSlotMapper.toEntity(any())).thenReturn(new TimeSlot());

        assertThrows(BadRequestException.class, () ->
                complexTimeSlotService.bulkCreateSlotsForFacility(100, request, ownerUser.getUserId()));

        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreateSlotsForComplex_Success() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(true)
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(17, 0))
                                .endTime(LocalTime.of(18, 0))
                                .pricePerSlot(BigDecimal.valueOf(200000))
                                .build()
                ))
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(10)).thenReturn(Optional.of(complex));
        when(stadiumRepository.findCourtsByComplexId(10)).thenReturn(List.of(court1, court2));
        when(timeSlotRepository.findOverlappingSlots(any(), any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotMapper.toEntity(any())).thenReturn(new TimeSlot());
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlotResponse> responses = complexTimeSlotService.bulkCreateSlotsForComplex(10, request, ownerUser.getUserId());

        assertNotNull(responses);
        verify(timeSlotRepository).saveAll(any());
    }

    @Test
    void bulkCreateSlotsForComplex_CustomCourtIds_Success() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(false)
                .courtIds(List.of(101))
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(17, 0))
                                .endTime(LocalTime.of(18, 0))
                                .pricePerSlot(BigDecimal.valueOf(200000))
                                .build()
                ))
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(10)).thenReturn(Optional.of(complex));
        when(stadiumRepository.findCourtsByIds(List.of(101))).thenReturn(List.of(court1));
        when(timeSlotRepository.findOverlappingSlots(any(), any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotMapper.toEntity(any())).thenReturn(new TimeSlot());
        when(timeSlotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TimeSlotResponse> responses = complexTimeSlotService.bulkCreateSlotsForComplex(10, request, ownerUser.getUserId());

        assertNotNull(responses);
        verify(timeSlotRepository).saveAll(any());
    }

    @Test
    void bulkCreateSlotsForComplex_OverlapRollback_ThrowsException() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(true)
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(9, 0))
                                .pricePerSlot(BigDecimal.valueOf(200000))
                                .build()
                ))
                .build();

        when(stadiumComplexRepository.findWithDetailsByComplexId(10)).thenReturn(Optional.of(complex));
        when(stadiumRepository.findCourtsByComplexId(10)).thenReturn(List.of(court1, court2));

        // Mock court2 bị trùng giờ (overlap), nên transaction sẽ rollback
        when(timeSlotRepository.findOverlappingSlots(eq(101), any(), any())).thenReturn(Collections.emptyList());
        when(timeSlotRepository.findOverlappingSlots(eq(102), any(), any())).thenReturn(List.of(new TimeSlot()));
        when(timeSlotMapper.toEntity(any())).thenReturn(new TimeSlot());

        assertThrows(BadRequestException.class, () ->
                complexTimeSlotService.bulkCreateSlotsForComplex(10, request, ownerUser.getUserId()));

        verify(timeSlotRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreateSlotsForFacility_CourtNotBelonging_ThrowsException() {
        // Court này thuộc facility khác (id 999)
        Stadium facility2 = Stadium.builder().stadiumId(999).nodeType(StadiumNodeType.FACILITY).build();
        Stadium externalCourt = Stadium.builder()
                .stadiumId(105)
                .nodeType(StadiumNodeType.COURT)
                .parentStadium(facility2)
                .build();

        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(false)
                .courtIds(List.of(105))
                .slots(List.of(
                        CreateTimeSlotRequest.builder()
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(9, 0))
                                .pricePerSlot(BigDecimal.valueOf(150000))
                                .build()
                ))
                .build();

        when(stadiumRepository.findFacilityWithComplexDetails(100)).thenReturn(Optional.of(facility));
        when(stadiumRepository.findCourtsByIds(List.of(105))).thenReturn(List.of(externalCourt));

        assertThrows(BadRequestException.class, () ->
                complexTimeSlotService.bulkCreateSlotsForFacility(100, request, ownerUser.getUserId()));
    }

    @Test
    void bulkCreateSlotsForFacility_FacilityNotFound_ThrowsException() {
        BulkTimeSlotRequest request = BulkTimeSlotRequest.builder()
                .applyToAllCourts(true)
                .build();

        when(stadiumRepository.findFacilityWithComplexDetails(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                complexTimeSlotService.bulkCreateSlotsForFacility(999, request, ownerUser.getUserId()));
    }
}
