package com.sportvenue.controller;

import com.sportvenue.dto.request.CreateSportTypeRequest;
import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.service.SportTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/sport-types")
@RequiredArgsConstructor
@Tag(name = "Admin Sport Type", description = "Admin endpoints for managing sport types")
@Slf4j
public class AdminSportTypeController {

    private final SportTypeService sportTypeService;

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Add new sport type", description = "Allows Admin to add a new sport category. Requires ROLE_Admin.")
    public ResponseEntity<SportTypeResponse> createSportType(@Valid @RequestBody CreateSportTypeRequest request) {
        log.info("Admin request to create sport type: {}", request.getSportName());
        SportTypeResponse response = sportTypeService.createSportType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
