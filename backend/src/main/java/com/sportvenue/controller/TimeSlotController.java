package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Time Slot", description = "Endpoints for managing stadium time slots")
@Slf4j
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping("/{stadiumId}/time-slots")
    @Operation(summary = "Get slots by stadium ID")
    public ResponseEntity<List<TimeSlotResponse>> getSlots(@PathVariable Integer stadiumId) {
        return ResponseEntity.ok(timeSlotService.getSlotsByStadiumId(stadiumId));
    }

    @PostMapping("/{stadiumId}/time-slots")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Create a single time slot")
    public ResponseEntity<TimeSlotResponse> createSlot(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateTimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                timeSlotService.createSlot(stadiumId, request, userPrincipal.getUser().getUserId()));
    }

    @PostMapping("/{stadiumId}/time-slots/bulk")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Bulk create time slots")
    public ResponseEntity<List<TimeSlotResponse>> bulkCreateSlots(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody List<CreateTimeSlotRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                timeSlotService.bulkCreateSlots(stadiumId, requests, userPrincipal.getUser().getUserId()));
    }

    @DeleteMapping("/time-slots/{slotId}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Delete a time slot")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Integer slotId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        timeSlotService.deleteSlot(slotId, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/time-slots/{slotId}/toggle")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Toggle slot status (Active/Inactive)")
    public ResponseEntity<TimeSlotResponse> toggleSlot(
            @PathVariable Integer slotId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(timeSlotService.toggleSlotStatus(slotId, userPrincipal.getUser().getUserId()));
    }
}
