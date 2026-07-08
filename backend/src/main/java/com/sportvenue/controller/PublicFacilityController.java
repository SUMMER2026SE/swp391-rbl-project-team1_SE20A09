package com.sportvenue.controller;

import com.sportvenue.dto.response.CourtResponse;
import com.sportvenue.service.PublicComplexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/public/facilities")
@RequiredArgsConstructor
@Tag(name = "PublicFacility", description = "Public endpoints for facilities")
@Slf4j
public class PublicFacilityController {

    private final PublicComplexService publicComplexService;

    @GetMapping("/{id}/courts")
    @Operation(summary = "Get courts under a facility", description = "Returns the list of courts (L3) under a specific facility.")
    public ResponseEntity<List<CourtResponse>> getCourtsByFacilityId(
            @PathVariable @Positive(message = "Facility ID must be positive") Integer id) {
        log.info("Public API call to get courts for facility ID: {}", id);
        List<CourtResponse> response = publicComplexService.getCourtsByFacilityId(id);
        return ResponseEntity.ok(response);
    }
}
