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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints bảo trì theo khung ngày ở cấp Complex (L1) — cascade xuống toàn bộ
 * Facility + Court con. Việc kết thúc sớm ({@code PATCH .../end}) dùng chung
 * endpoint với Stadium tại {@link MaintenanceScheduleController}.
 *
 * <p>Sibling của {@link MaintenanceScheduleController} — xem Javadoc bên đó về lý do tách 2
 * controller và lưu ý giữ đồng bộ khi sửa 1 trong 2.</p>
 */
@RestController
@RequestMapping("/api/v1/complexes")
@RequiredArgsConstructor
@Tag(name = "Maintenance Schedule", description = "Endpoints for managing dated maintenance schedules of a stadium complex")
@Slf4j
public class ComplexMaintenanceScheduleController {

    private final MaintenanceScheduleService maintenanceScheduleService;

    @PostMapping("/{complexId}/maintenance-schedules")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Create a dated maintenance schedule for a complex (cascades to all facilities/courts)")
    public ResponseEntity<MaintenanceScheduleResponse> createComplexSchedule(
            @PathVariable Integer complexId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateMaintenanceScheduleRequest request) {

        log.info("Owner {} request create maintenance schedule for complex {} — {} -> {}",
                userPrincipal.getUser().getUserId(), complexId, request.getStartDate(), request.getEndDate());
        return ResponseEntity.ok(maintenanceScheduleService.createComplexSchedule(
                complexId, request, userPrincipal.getUser().getUserId()));
    }

    @GetMapping("/{complexId}/maintenance-schedules")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "List maintenance schedule history for a complex (owner-only, paginated)")
    public ResponseEntity<Page<MaintenanceScheduleResponse>> listComplexSchedules(
            @PathVariable Integer complexId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(maintenanceScheduleService.listComplexSchedules(
                complexId, userPrincipal.getUser().getUserId(), pageable));
    }
}
