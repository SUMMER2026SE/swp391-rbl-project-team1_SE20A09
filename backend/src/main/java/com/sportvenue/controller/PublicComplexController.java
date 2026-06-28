package com.sportvenue.controller;

import com.sportvenue.dto.response.FacilityResponse;
import com.sportvenue.dto.response.PublicComplexDetailResponse;
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
@RequestMapping("/api/v1/public/complexes")
@RequiredArgsConstructor
@Tag(name = "PublicComplex", description = "Public endpoints for sport complexes")
@Slf4j
public class PublicComplexController {

    private final PublicComplexService publicComplexService;

    @GetMapping("/{id}")
    @Operation(summary = "Get public complex detail by ID", description = "Returns details of a specific complex, public access.")
    public ResponseEntity<PublicComplexDetailResponse> getPublicComplexById(
            @PathVariable @Positive(message = "Complex ID must be positive") Integer id) {
        log.info("Public API call to get complex detail for ID: {}", id);
        PublicComplexDetailResponse response = publicComplexService.getPublicComplexById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/facilities")
    @Operation(summary = "Get facilities under a complex", description = "Returns the list of facilities (L2) under a specific complex.")
    public ResponseEntity<List<FacilityResponse>> getFacilitiesByComplexId(
            @PathVariable @Positive(message = "Complex ID must be positive") Integer id) {
        log.info("Public API call to get facilities for complex ID: {}", id);
        List<FacilityResponse> response = publicComplexService.getFacilitiesByComplexId(id);
        return ResponseEntity.ok(response);
    }
}
