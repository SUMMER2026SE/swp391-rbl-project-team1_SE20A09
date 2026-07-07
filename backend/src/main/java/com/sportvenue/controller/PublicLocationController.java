package com.sportvenue.controller;

import com.sportvenue.dto.response.SupportedLocationDto;
import com.sportvenue.util.location.VietnamLocationReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tách riêng khỏi PublicComplexController (đã có @RequestMapping("/api/v1/public/complexes"))
 * để đường dẫn là /api/v1/public/locations thay vì bị lồng thành /complexes/locations.
 */
@RestController
@RequestMapping("/api/v1/public/locations")
@Tag(name = "PublicLocation", description = "Static province/district reference data for search dropdowns")
public class PublicLocationController {

    @GetMapping
    @Operation(summary = "Get supported provinces/districts",
            description = "Returns the 2 provinces (Hồ Chí Minh, Đà Nẵng) currently supported for search, each with their districts.")
    public ResponseEntity<List<SupportedLocationDto>> getLocations() {
        return ResponseEntity.ok(VietnamLocationReference.toSupportedLocationDtos());
    }
}
