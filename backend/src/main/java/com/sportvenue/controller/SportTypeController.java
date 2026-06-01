package com.sportvenue.controller;

import com.sportvenue.dto.response.SportTypeResponse;
import com.sportvenue.service.SportTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sport-types")
@RequiredArgsConstructor
@Tag(name = "Sport Type", description = "Endpoints for managing sport types")
public class SportTypeController {

    private final SportTypeService sportTypeService;

    @GetMapping
    @Operation(summary = "Get all sport types")
    public ResponseEntity<List<SportTypeResponse>> getAllSportTypes() {
        return ResponseEntity.ok(sportTypeService.getAllSportTypes());
    }
}
