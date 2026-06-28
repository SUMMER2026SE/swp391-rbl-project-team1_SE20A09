package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateComplexRequest;
import com.sportvenue.dto.response.ComplexResponse;
import com.sportvenue.security.RequireApprovedOwner;
import com.sportvenue.security.UserPrincipal;
import com.sportvenue.service.StadiumComplexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/complexes")
@RequiredArgsConstructor
@Tag(name = "StadiumComplex", description = "Endpoints for managing sport complexes (L1)")
@Slf4j
public class StadiumComplexController {

    private final StadiumComplexService stadiumComplexService;

    @PostMapping
    @PreAuthorize("hasRole('Owner')")
    @RequireApprovedOwner
    @Operation(summary = "Add new complex", description = "Allows an owner to create a new stadium complex (L1).")
    public ResponseEntity<ComplexResponse> createComplex(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateComplexRequest request) {
        log.info("Received request to create complex: {} from owner: {}", request.getName(), userPrincipal.getUsername());
        ComplexResponse response = stadiumComplexService.createComplex(request, userPrincipal.getUser().getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('Owner')")
    @RequireApprovedOwner
    @Operation(summary = "Get my complexes", description = "Returns all complexes owned by the authenticated owner.")
    public ResponseEntity<List<ComplexResponse>> getMyComplexes(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<ComplexResponse> complexes = stadiumComplexService.getMyComplexes(userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(complexes);
    }

    @GetMapping("/{complexId}")
    @PreAuthorize("hasRole('Owner')")
    @RequireApprovedOwner
    @Operation(summary = "Get complex by ID", description = "Returns details of a specific complex owned by the authenticated owner.")
    public ResponseEntity<ComplexResponse> getComplexById(
            @PathVariable Integer complexId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ComplexResponse complex = stadiumComplexService.getComplexByIdAndOwner(complexId, userPrincipal.getUser().getUserId());
        return ResponseEntity.ok(complex);
    }

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Get all complexes (Admin)", description = "Allows Admin to retrieve all complexes in the system with optional status filter.")
    public ResponseEntity<List<ComplexResponse>> getAllComplexes(
            @RequestParam(required = false) String approvedStatus) {
        log.info("Admin request to get all complexes with status: {}", approvedStatus);
        List<ComplexResponse> complexes = stadiumComplexService.getAllComplexes(approvedStatus);
        return ResponseEntity.ok(complexes);
    }

    @PutMapping("/{complexId}/approve")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Approve complex", description = "Allows Admin to approve a complex.")
    public ResponseEntity<ComplexResponse> approveComplex(@PathVariable Integer complexId) {
        log.info("Admin request to approve complex ID: {}", complexId);
        ComplexResponse response = stadiumComplexService.approveComplex(complexId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{complexId}/reject")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Reject complex", description = "Allows Admin to reject a complex.")
    public ResponseEntity<ComplexResponse> rejectComplex(
            @PathVariable Integer complexId,
            @RequestParam String reason) {
        log.info("Admin request to reject complex ID: {} with reason: {}", complexId, reason);
        ComplexResponse response = stadiumComplexService.rejectComplex(complexId, reason);
        return ResponseEntity.ok(response);
    }
}
