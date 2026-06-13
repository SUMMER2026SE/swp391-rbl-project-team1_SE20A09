package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateStadiumRequest;
import com.sportvenue.dto.request.UpdateStadiumRequest;
import com.sportvenue.dto.response.StadiumResponse;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.StadiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
@RequiredArgsConstructor
@Tag(name = "Stadium", description = "Endpoints for managing sport venues (stadiums)")
@Slf4j
public class StadiumController {

    private final StadiumService stadiumService;

    @PostMapping
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Add new stadium", description = "Allows an owner to add a new sport venue. Requires ROLE_Owner.")
    public ResponseEntity<StadiumResponse> createStadium(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateStadiumRequest request) {
        log.info("Received request to create stadium: {} from owner: {}", request.getStadiumName(), userPrincipal.getUsername());
        StadiumResponse response = stadiumService.createStadium(request, userPrincipal.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Get my stadiums", description = "Returns all stadiums owned by the authenticated owner.")
    public ResponseEntity<List<StadiumResponse>> getMyStadiums(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer sportTypeId,
            @RequestParam(required = false) String status) {
        List<StadiumResponse> stadiums = stadiumService.getMyStadiums(
                userPrincipal.getUser().getUserId(), search, sportTypeId, status);
        return ResponseEntity.ok(stadiums);
    }

    @PutMapping("/{stadiumId}/suspend")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Suspend stadium", description = "Allows an owner to suspend their stadium (set status to MAINTENANCE)")
    public ResponseEntity<Void> suspendStadium(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Request to suspend stadium: {} from owner: {}", stadiumId, userPrincipal.getUsername());
        stadiumService.suspendStadium(stadiumId, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{stadiumId}/activate")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Activate stadium", description = "Allows an owner to activate their suspended stadium (set status to AVAILABLE)")
    public ResponseEntity<Void> activateStadium(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Request to activate stadium: {} from owner: {}", stadiumId, userPrincipal.getUsername());
        stadiumService.activateStadium(stadiumId, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{stadiumId}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Delete stadium", description = "Allows an owner to soft-delete their stadium (set status to CLOSED)")
    public ResponseEntity<Void> deleteStadium(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Request to delete stadium: {} from owner: {}", stadiumId, userPrincipal.getUsername());
        stadiumService.deleteStadium(stadiumId, userPrincipal.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{stadiumId}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Get stadium by ID", description = "Returns a specific stadium owned by the authenticated owner.")
    public ResponseEntity<StadiumResponse> getStadiumById(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        StadiumResponse stadium = stadiumService.getStadiumByIdAndOwner(
                stadiumId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(stadium);
    }

    @PutMapping("/{stadiumId}")
    @PreAuthorize("hasRole('Owner')")
    @Operation(summary = "Update stadium", description = "Allows an owner to update their stadium. Ownership is verified.")
    public ResponseEntity<StadiumResponse> updateStadium(
            @PathVariable Integer stadiumId,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateStadiumRequest request) {
        log.info("Received request to update stadium ID: {} from owner: {}", stadiumId, userPrincipal.getUsername());
        StadiumResponse response = stadiumService.updateStadium(stadiumId, request, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Get all stadiums (Admin)", description = "Allows system Admin to get all stadiums in the system with optional status filtering.")
    public ResponseEntity<List<StadiumResponse>> getAllStadiums(
            @RequestParam(required = false) String approvedStatus) {
        log.info("Admin request to get all stadiums with approvedStatus filter: {}", approvedStatus);
        List<StadiumResponse> stadiums = stadiumService.getAllStadiums(approvedStatus);
        return ResponseEntity.ok(stadiums);
    }

    @PutMapping("/{stadiumId}/approve")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Approve stadium", description = "Allows Admin to approve a stadium to make it visible in public searches.")
    public ResponseEntity<StadiumResponse> approveStadium(@PathVariable Integer stadiumId) {
        log.info("Admin request to approve stadium ID: {}", stadiumId);
        StadiumResponse response = stadiumService.approveStadium(stadiumId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{stadiumId}/reject")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Reject stadium", description = "Allows Admin to reject a stadium.")
    public ResponseEntity<StadiumResponse> rejectStadium(@PathVariable Integer stadiumId) {
        log.info("Admin request to reject stadium ID: {}", stadiumId);
        StadiumResponse response = stadiumService.rejectStadium(stadiumId);
        return ResponseEntity.ok(response);
    }
}
