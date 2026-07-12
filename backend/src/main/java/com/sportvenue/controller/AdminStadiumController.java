package com.sportvenue.controller;

import com.sportvenue.dto.request.AdminSuspendStadiumRequest;
import com.sportvenue.dto.response.ApiResponse;
import com.sportvenue.service.StadiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stadiums")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Stadium Management", description = "Admin-only stadium moderation endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('Admin')")
public class AdminStadiumController {

    private final StadiumService stadiumService;

    @PatchMapping("/{stadiumId}/suspend")
    @Operation(
            summary = "Suspend a single stadium",
            description = "Allows Admin to suspend one stadium without locking the Owner account."
    )
    public ResponseEntity<ApiResponse<Void>> suspendStadium(
            @Parameter(description = "Stadium ID", example = "1") @PathVariable Integer stadiumId,
            @RequestBody(required = false) AdminSuspendStadiumRequest request) {
        String reason = request != null ? request.getReason() : null;
        stadiumService.suspendStadiumByAdmin(stadiumId, reason);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Stadium suspended successfully")
                .build());
    }

    @PatchMapping("/{stadiumId}/unsuspend")
    @Operation(
            summary = "Unsuspend a single stadium",
            description = "Allows Admin to clear an Admin suspension and reactivate one stadium."
    )
    public ResponseEntity<ApiResponse<Void>> unsuspendStadium(
            @Parameter(description = "Stadium ID", example = "1") @PathVariable Integer stadiumId) {
        stadiumService.unsuspendStadiumByAdmin(stadiumId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Stadium unsuspended successfully")
                .build());
    }
}
