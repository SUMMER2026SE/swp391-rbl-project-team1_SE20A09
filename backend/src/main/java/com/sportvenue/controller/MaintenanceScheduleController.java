package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateMaintenanceScheduleRequest;
import com.sportvenue.dto.response.MaintenanceScheduleResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.MaintenanceScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Maintenance Schedule", description = "Endpoints for managing dated maintenance schedules of a stadium")
@Slf4j
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping("/{stadiumId}/maintenance-schedules")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Create a dated maintenance schedule for a stadium (cascades FACILITY -> child courts)")
    public ResponseEntity<MaintenanceScheduleResponse> createSchedule(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateMaintenanceScheduleRequest request) {

        log.info("Owner {} request create maintenance schedule for stadium {} — {} -> {}",
                userPrincipal.getUser().getUserId(), stadiumId, request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(maintenanceScheduleService.createSchedule(
                stadiumId, request, userPrincipal.getUser().getUserId()));
    }

    @GetMapping("/{stadiumId}/maintenance-schedules")
    @Operation(summary = "List maintenance schedule history for a stadium")
    public ResponseEntity<List<MaintenanceScheduleResponse>> listSchedules(@PathVariable Integer stadiumId) {
        return ResponseEntity.ok(maintenanceScheduleService.listSchedules(stadiumId));
    }

    @PatchMapping("/maintenance-schedules/{maintenanceId}/end")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "End an active maintenance schedule immediately")
    public ResponseEntity<Void> endSchedule(
            @PathVariable Integer maintenanceId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Owner {} request end maintenance schedule {}", userPrincipal.getUser().getUserId(), maintenanceId);
        maintenanceScheduleService.endSchedule(maintenanceId, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }
}
