package com.sportvenue.service;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.Owner;
import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.TimeSlot;
import com.sportvenue.entity.User;
import com.sportvenue.entity.enums.SlotStatus;
import com.sportvenue.exception.BadRequestException;
import com.sportvenue.exception.ResourceNotFoundException;
import com.sportvenue.mapper.TimeSlotMapper;
import com.sportvenue.repository.StadiumRepository;
import com.sportvenue.repository.TimeSlotRepository;
import com.sportvenue.service.impl.TimeSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceImplTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @InjectMocks
    private TimeSlotServiceImpl timeSlotService;

    private Stadium stadium;
    private User ownerUser;
    private Owner owner;

    @BeforeEach
    void setUp() {
        ownerUser = User.builder().userId(1).email("owner@test.com").build();
        owner = Owner.builder().ownerId(1).user(ownerUser).build();
        stadium = Stadium.builder()
                .stadiumId(1)
                .stadiumName("Test Stadium")
                .owner(owner)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .timeSlots(new java.util.LinkedHashSet<>())
                .build();
    }

    @Test
    void createSlot_Success() {
        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .pricePerSlot(BigDecimal.valueOf(100000))
                .build();

        TimeSlot entity = new TimeSlot();
        entity.setSlotId(1);
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());

        when(stadiumRepository.findById(1)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findOverlappingSlots(eq(1), any(), any())).thenReturn(List.of());
        when(timeSlotMapper.toEntity(request)).thenReturn(entity);
        when(timeSlotRepository.save(any())).thenReturn(entity);
        when(timeSlotMapper.toResponse(entity)).thenReturn(new TimeSlotResponse());

        assertNotNull(timeSlotService.createSlot(1, request, ownerUser.getUserId()));
        verify(timeSlotRepository).save(any());
    }

    @Test
    void createSlot_OutsideHours_ThrowsException() {
        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(7, 0)) // Stadium opens at 8
                .endTime(LocalTime.of(9, 0))
                .build();

        when(stadiumRepository.findById(1)).thenReturn(Optional.of(stadium));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
                timeSlotService.createSlot(1, request, ownerUser.getUserId()));
        assertTrue(ex.getMessage().contains("outside stadium operating hours"));
    }

    @Test
    void createSlot_Overlap_ThrowsException() {
        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        TimeSlot existing = TimeSlot.builder()
                .startTime(LocalTime.of(9, 30))
                .endTime(LocalTime.of(10, 30))
                .build();

        when(stadiumRepository.findById(1)).thenReturn(Optional.of(stadium));
        when(timeSlotRepository.findOverlappingSlots(1, request.getStartTime(), request.getEndTime()))
                .thenReturn(List.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
                timeSlotService.createSlot(1, request, ownerUser.getUserId()));
        assertTrue(ex.getMessage().contains("overlaps with existing slot"));
    }

    @Test
    void bulkCreateSlots_InternalOverlap_ThrowsException() {
        List<CreateTimeSlotRequest> requests = List.of(
                CreateTimeSlotRequest.builder().startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).build(),
                CreateTimeSlotRequest.builder().startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build()
        );

        when(stadiumRepository.findById(1)).thenReturn(Optional.of(stadium));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
                timeSlotService.bulkCreateSlots(1, requests, ownerUser.getUserId()));
        assertTrue(ex.getMessage().contains("Overlapping slots in request"));
    }

    @Test
    void deleteSlot_Booked_ThrowsException() {
        TimeSlot slot = TimeSlot.builder()
                .slotId(1)
                .stadium(stadium)
                .slotStatus(SlotStatus.BOOKED)
                .build();

        when(timeSlotRepository.findById(1)).thenReturn(Optional.of(slot));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
                timeSlotService.deleteSlot(1, ownerUser.getUserId()));
        assertTrue(ex.getMessage().contains("Cannot delete a booked slot"));
    }

    @Test
    void deleteSlot_WrongOwner_ThrowsException() {
        TimeSlot slot = TimeSlot.builder()
                .slotId(1)
                .stadium(stadium)
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        when(timeSlotRepository.findById(1)).thenReturn(Optional.of(slot));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
                timeSlotService.deleteSlot(1, 999)); // Different user ID
        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void toggleSlotStatus_Success() {
        TimeSlot slot = TimeSlot.builder()
                .slotId(1)
                .stadium(stadium)
                .slotStatus(SlotStatus.AVAILABLE)
                .build();

        when(timeSlotRepository.findById(1)).thenReturn(Optional.of(slot));
        when(timeSlotRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(timeSlotMapper.toResponse(any())).thenReturn(new TimeSlotResponse());

        timeSlotService.toggleSlotStatus(1, ownerUser.getUserId());
        assertEquals(SlotStatus.MAINTENANCE, slot.getSlotStatus());

        timeSlotService.toggleSlotStatus(1, ownerUser.getUserId());
        assertEquals(SlotStatus.AVAILABLE, slot.getSlotStatus());
    }
}
