package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.entity.User;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.TimeSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotControllerTest {

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private TimeSlotController timeSlotController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        User user = User.builder().userId(1).email("owner@test.com").build();
        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void createSlot_Success() {
        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .pricePerSlot(BigDecimal.valueOf(100000))
                .build();

        TimeSlotResponse response = TimeSlotResponse.builder().slotId(1).build();

        when(timeSlotService.createSlot(eq(1), any(), eq(1))).thenReturn(response);

        ResponseEntity<TimeSlotResponse> result = timeSlotController.createSlot(1, userPrincipal, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1, result.getBody().getSlotId());
        verify(timeSlotService).createSlot(1, request, 1);
    }

    @Test
    void bulkCreateSlots_Success() {
        List<CreateTimeSlotRequest> requests = List.of(new CreateTimeSlotRequest());
        when(timeSlotService.bulkCreateSlots(eq(1), any(), eq(1))).thenReturn(List.of(new TimeSlotResponse()));

        ResponseEntity<List<TimeSlotResponse>> result = timeSlotController.bulkCreateSlots(1, userPrincipal, requests);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(timeSlotService).bulkCreateSlots(1, requests, 1);
    }

    @Test
    void deleteSlot_Success() {
        ResponseEntity<Void> result = timeSlotController.deleteSlot(1, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(timeSlotService).deleteSlot(1, 1);
    }

    @Test
    void toggleSlot_Success() {
        when(timeSlotService.toggleSlotStatus(eq(1), eq(1))).thenReturn(new TimeSlotResponse());

        ResponseEntity<TimeSlotResponse> result = timeSlotController.toggleSlot(1, userPrincipal);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(timeSlotService).toggleSlotStatus(1, 1);
    }
}
