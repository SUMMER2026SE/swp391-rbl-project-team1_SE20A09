package com.sportvenue.controller;

import com.sportvenue.dto.request.BulkTimeSlotRequest;
import com.sportvenue.dto.response.TimeSlotResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.ComplexTimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/owner")
@RequiredArgsConstructor
@Tag(name = "Complex/Facility Time Slot", description = "Endpoints for owner to bulk manage time slots across complex or facility")
@Slf4j
@Validated
public class ComplexTimeSlotController {

    private final ComplexTimeSlotService complexTimeSlotService;

    @PostMapping("/facilities/{facilityId}/time-slots/bulk")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Bulk create time slots for courts under a specific Facility")
    public ResponseEntity<List<TimeSlotResponse>> bulkCreateForFacility(
            @PathVariable Integer facilityId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BulkTimeSlotRequest request) {
        log.info("REST request to bulk create slots for facility ID: {}", facilityId);
        List<TimeSlotResponse> responses = complexTimeSlotService.bulkCreateSlotsForFacility(
                facilityId, request, userPrincipal.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/complexes/{complexId}/time-slots/bulk")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Bulk create time slots for courts under a specific Complex")
    public ResponseEntity<List<TimeSlotResponse>> bulkCreateForComplex(
            @PathVariable Integer complexId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody BulkTimeSlotRequest request) {
        log.info("REST request to bulk create slots for complex ID: {}", complexId);
        List<TimeSlotResponse> responses = complexTimeSlotService.bulkCreateSlotsForComplex(
                complexId, request, userPrincipal.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
}
