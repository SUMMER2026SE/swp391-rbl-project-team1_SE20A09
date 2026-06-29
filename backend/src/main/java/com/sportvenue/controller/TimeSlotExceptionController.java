package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateExceptionRequest;
import com.sportvenue.dto.response.TimeSlotExceptionResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.TimeSlotExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stadiums/time-slots")
@RequiredArgsConstructor
@Tag(name = "Time Slot Exception", description = "Endpoints for managing time slot exceptions by date")
@Slf4j
@Validated
public class TimeSlotExceptionController {

    private final TimeSlotExceptionService timeSlotExceptionService;

    @PostMapping("/{slotId}/exceptions/{date}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Create or update exception for a slot on a specific date")
    public ResponseEntity<TimeSlotExceptionResponse> createOrUpdateException(
            @PathVariable Integer slotId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateExceptionRequest request) {
        
        log.info("Request exception for slot {} on date {} — priceOverride={}, closed={}", slotId, date, request.getPriceOverride(), request.getClosed());
        return ResponseEntity.ok(timeSlotExceptionService.createOrUpdateException(
                slotId, date, request, userPrincipal.getUser().getUserId()));
    }

    @DeleteMapping("/{slotId}/exceptions/{date}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Delete exception for a slot on a specific date")
    public ResponseEntity<Void> deleteException(
            @PathVariable Integer slotId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("Delete exception for slot {} on date {}", slotId, date);
        timeSlotExceptionService.deleteException(slotId, date, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }
}
